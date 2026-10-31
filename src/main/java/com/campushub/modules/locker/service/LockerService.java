package com.campushub.modules.locker.service;

import com.campushub.common.exception.AppException;
import com.campushub.common.exception.ErrorCode;
import com.campushub.modules.locker.dto.*;
import com.campushub.modules.locker.entity.Compartment;
import com.campushub.modules.locker.entity.CompartmentStatus;
import com.campushub.modules.locker.entity.Locker;
import com.campushub.modules.locker.entity.LockerStatus;
import com.campushub.modules.locker.repository.CompartmentRepository;
import com.campushub.modules.locker.repository.LockerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class LockerService {

    private final LockerRepository lockerRepository;
    private final CompartmentRepository compartmentRepository;
    private final com.campushub.modules.order.repository.OrderRepository orderRepository;

    public LockerService(LockerRepository lockerRepository, CompartmentRepository compartmentRepository,
                         com.campushub.modules.order.repository.OrderRepository orderRepository) {
        this.lockerRepository = lockerRepository;
        this.compartmentRepository = compartmentRepository;
        this.orderRepository = orderRepository;
    }

    private CompartmentResponse mapCompartment(Compartment compartment) {
        CompartmentResponse res = new CompartmentResponse();
        res.setId(compartment.getId());
        res.setLockerId(compartment.getLocker().getId());
        res.setCode(compartment.getCode());
        res.setStatus(compartment.getStatus());
        res.setCreatedAt(compartment.getCreatedAt());
        res.setUpdatedAt(compartment.getUpdatedAt());
        return res;
    }

    private LockerResponse mapLocker(Locker locker) {
        LockerResponse res = new LockerResponse();
        res.setId(locker.getId());
        res.setCode(locker.getCode());
        res.setName(locker.getName());
        res.setLocation(locker.getLocation());
        res.setStatus(locker.getStatus());
        res.setCreatedAt(locker.getCreatedAt());
        res.setUpdatedAt(locker.getUpdatedAt());

        res.setTotalCompartments(compartmentRepository.countByLockerId(locker.getId()));
        res.setAvailableCompartments(compartmentRepository.countByLockerIdAndStatus(locker.getId(), CompartmentStatus.AVAILABLE));
        res.setOccupiedCompartments(compartmentRepository.countByLockerIdAndStatus(locker.getId(), CompartmentStatus.OCCUPIED));
        return res;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "lockers", key = "'active_all'")
    public List<LockerResponse> getActiveLockers() {
        return lockerRepository.findByStatus(LockerStatus.ACTIVE).stream()
                .map(this::mapLocker)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<LockerResponse> getLockers(LockerStatus status, String search, Pageable pageable) {
        String searchPattern = (search == null || search.trim().isEmpty()) ? null : "%" + search.trim().toLowerCase() + "%";
        return lockerRepository.findAllWithFilters(status, searchPattern, pageable)
                .map(this::mapLocker);
    }

    @Transactional(readOnly = true)
    public LockerDetailResponse getLocker(Long id) {
        Locker locker = lockerRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.LOCKER_NOT_FOUND));

        List<Compartment> compartments = compartmentRepository.findByLockerIdOrderByCodeAsc(id);
        List<CompartmentResponse> compartmentResponses = compartments.stream()
                .map(this::mapCompartment)
                .collect(Collectors.toList());

        long total = compartments.size();
        long available = compartments.stream().filter(c -> c.getStatus() == CompartmentStatus.AVAILABLE).count();
        long reserved = compartments.stream().filter(c -> c.getStatus() == CompartmentStatus.RESERVED).count();
        long occupied = compartments.stream().filter(c -> c.getStatus() == CompartmentStatus.OCCUPIED).count();
        long error = compartments.stream().filter(c -> c.getStatus() == CompartmentStatus.ERROR).count();
        long outOfService = compartments.stream().filter(c -> c.getStatus() == CompartmentStatus.OUT_OF_SERVICE).count();

        CompartmentSummaryResponse summary = new CompartmentSummaryResponse(total, available, reserved, occupied, error, outOfService);

        LockerDetailResponse res = new LockerDetailResponse();
        res.setId(locker.getId());
        res.setCode(locker.getCode());
        res.setName(locker.getName());
        res.setLocation(locker.getLocation());
        res.setStatus(locker.getStatus());
        res.setCreatedAt(locker.getCreatedAt());
        res.setUpdatedAt(locker.getUpdatedAt());
        res.setCompartmentSummary(summary);
        res.setCompartments(compartmentResponses);
        return res;
    }

    @Transactional
    @CacheEvict(value = "lockers", allEntries = true)
    public LockerResponse createLocker(LockerCreateRequest request) {
        String code = request.getCode().trim().toUpperCase();
        if (lockerRepository.existsByCode(code)) {
            throw new AppException(ErrorCode.LOCKER_CODE_ALREADY_EXISTS);
        }

        Locker locker = new Locker();
        locker.setCode(code);
        locker.setName(request.getName().trim());
        locker.setLocation(request.getLocation().trim());
        locker.setStatus(LockerStatus.ACTIVE);

        locker = lockerRepository.save(locker);
        return mapLocker(locker);
    }

    @Transactional
    @CacheEvict(value = "lockers", allEntries = true)
    public LockerResponse updateLocker(Long id, LockerUpdateRequest request) {
        Locker locker = lockerRepository.findByIdWithLock(id)
                .orElseThrow(() -> new AppException(ErrorCode.LOCKER_NOT_FOUND));

        locker.setName(request.getName().trim());
        locker.setLocation(request.getLocation().trim());

        locker = lockerRepository.save(locker);
        return mapLocker(locker);
    }

    @Transactional
    @CacheEvict(value = "lockers", allEntries = true)
    public LockerResponse updateLockerStatus(Long id, LockerStatusUpdateRequest request) {
        Locker locker = lockerRepository.findByIdWithLock(id)
                .orElseThrow(() -> new AppException(ErrorCode.LOCKER_NOT_FOUND));

        LockerStatus current = locker.getStatus();
        LockerStatus target = request.getStatus();

        // Validate status transition
        validateLockerStatusTransition(current, target);

        locker.setStatus(target);
        locker = lockerRepository.save(locker);
        return mapLocker(locker);
    }

    /**
     * Validate locker status transitions.
     * Valid transitions:
     * - ACTIVE ↔ INACTIVE
     * - ACTIVE → MAINTENANCE
     * - INACTIVE → MAINTENANCE
     * - MAINTENANCE → ACTIVE
     * - MAINTENANCE → INACTIVE
     * - Any status → OFFLINE (emergency)
     * - OFFLINE → MAINTENANCE (recovery)
     */
    private void validateLockerStatusTransition(LockerStatus current, LockerStatus target) {
        if (current == target) {
            return; // No change
        }

        boolean isValid = switch (current) {
            case ACTIVE -> target == LockerStatus.INACTIVE 
                        || target == LockerStatus.MAINTENANCE 
                        || target == LockerStatus.OFFLINE;
            case INACTIVE -> target == LockerStatus.ACTIVE 
                          || target == LockerStatus.MAINTENANCE 
                          || target == LockerStatus.OFFLINE;
            case MAINTENANCE -> target == LockerStatus.ACTIVE 
                             || target == LockerStatus.INACTIVE 
                             || target == LockerStatus.OFFLINE;
            case OFFLINE -> target == LockerStatus.MAINTENANCE; // Only to MAINTENANCE for recovery
        };

        if (!isValid) {
            throw new AppException(ErrorCode.INVALID_LOCKER_STATUS);
        }
    }

    @Transactional(readOnly = true)
    public List<CompartmentResponse> getCompartments(Long lockerId) {
        if (!lockerRepository.existsById(lockerId)) {
            throw new AppException(ErrorCode.LOCKER_NOT_FOUND);
        }
        return compartmentRepository.findByLockerIdOrderByCodeAsc(lockerId)
                .stream().map(this::mapCompartment).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CompartmentResponse getCompartment(Long lockerId, Long compartmentId) {
        if (!lockerRepository.existsById(lockerId)) {
            throw new AppException(ErrorCode.LOCKER_NOT_FOUND);
        }
        Compartment compartment = compartmentRepository.findByIdAndLockerId(compartmentId, lockerId)
                .orElseThrow(() -> new AppException(ErrorCode.COMPARTMENT_NOT_FOUND));
        return mapCompartment(compartment);
    }

    @Transactional
    @CacheEvict(value = "lockers", allEntries = true)
    public CompartmentResponse createCompartment(Long lockerId, CompartmentCreateRequest request) {
        Locker locker = lockerRepository.findById(lockerId)
                .orElseThrow(() -> new AppException(ErrorCode.LOCKER_NOT_FOUND));

        String code = request.getCode().trim().toUpperCase();
        if (compartmentRepository.existsByLockerIdAndCode(lockerId, code)) {
            throw new AppException(ErrorCode.COMPARTMENT_CODE_ALREADY_EXISTS);
        }

        Compartment compartment = new Compartment();
        compartment.setLocker(locker);
        compartment.setCode(code);
        compartment.setStatus(CompartmentStatus.AVAILABLE);

        compartment = compartmentRepository.save(compartment);
        return mapCompartment(compartment);
    }

    @Transactional
    @CacheEvict(value = "lockers", allEntries = true)
    public CompartmentResponse updateCompartmentStatus(Long lockerId, Long compartmentId, CompartmentStatusUpdateRequest request) {
        if (!lockerRepository.existsById(lockerId)) {
            throw new AppException(ErrorCode.LOCKER_NOT_FOUND);
        }

        Compartment compartment = compartmentRepository.findByIdWithLock(compartmentId)
                .orElseThrow(() -> new AppException(ErrorCode.COMPARTMENT_NOT_FOUND));

        if (!compartment.getLocker().getId().equals(lockerId)) {
            throw new AppException(ErrorCode.COMPARTMENT_NOT_FOUND);
        }
        if (orderRepository.hasCompartmentReservation(compartmentId, java.util.Set.of(
                com.campushub.modules.order.entity.OrderStatus.AWAITING_DEPOSIT,
                com.campushub.modules.order.entity.OrderStatus.READY_FOR_PICKUP))) {
            throw new AppException(ErrorCode.UNSAFE_COMPARTMENT_STATUS_TRANSITION);
        }

        CompartmentStatus current = compartment.getStatus();
        CompartmentStatus target = request.getStatus();

        // Safe transition rule 1: Cannot manually free OCCUPIED or RESERVED compartments
        if (current == CompartmentStatus.OCCUPIED || current == CompartmentStatus.RESERVED) {
            throw new AppException(ErrorCode.UNSAFE_COMPARTMENT_STATUS_TRANSITION);
        }

        // Safe transition rule 2: Administrative updates cannot manually set OCCUPIED or RESERVED (belongs to Order/Pickup lifecycle)
        if (target == CompartmentStatus.OCCUPIED || target == CompartmentStatus.RESERVED) {
            throw new AppException(ErrorCode.INVALID_COMPARTMENT_STATUS);
        }

        compartment.setStatus(target);
        compartment = compartmentRepository.save(compartment);
        return mapCompartment(compartment);
    }
}
