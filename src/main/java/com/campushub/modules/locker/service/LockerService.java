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

import java.util.List;
import java.util.stream.Collectors;

@Service
public class LockerService {

    private final LockerRepository lockerRepository;
    private final CompartmentRepository compartmentRepository;

    public LockerService(LockerRepository lockerRepository, CompartmentRepository compartmentRepository) {
        this.lockerRepository = lockerRepository;
        this.compartmentRepository = compartmentRepository;
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
    public Page<LockerResponse> getLockers(LockerStatus status, String search, Pageable pageable) {
        String searchPattern = (search == null || search.trim().isEmpty()) ? null : "%" + search.trim() + "%";
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
    public LockerResponse updateLocker(Long id, LockerUpdateRequest request) {
        Locker locker = lockerRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.LOCKER_NOT_FOUND));

        locker.setName(request.getName().trim());
        locker.setLocation(request.getLocation().trim());

        locker = lockerRepository.save(locker);
        return mapLocker(locker);
    }

    @Transactional
    public LockerResponse updateLockerStatus(Long id, LockerStatusUpdateRequest request) {
        Locker locker = lockerRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.LOCKER_NOT_FOUND));

        locker.setStatus(request.getStatus());
        locker = lockerRepository.save(locker);
        return mapLocker(locker);
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
    public CompartmentResponse updateCompartmentStatus(Long lockerId, Long compartmentId, CompartmentStatusUpdateRequest request) {
        if (!lockerRepository.existsById(lockerId)) {
            throw new AppException(ErrorCode.LOCKER_NOT_FOUND);
        }

        Compartment compartment = compartmentRepository.findByIdAndLockerId(compartmentId, lockerId)
                .orElseThrow(() -> new AppException(ErrorCode.COMPARTMENT_NOT_FOUND));

        CompartmentStatus current = compartment.getStatus();
        CompartmentStatus target = request.getStatus();

        // Safe transition rule 1: Cannot manually free OCCUPIED or RESERVED compartments
        if ((current == CompartmentStatus.OCCUPIED || current == CompartmentStatus.RESERVED) && target == CompartmentStatus.AVAILABLE) {
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
