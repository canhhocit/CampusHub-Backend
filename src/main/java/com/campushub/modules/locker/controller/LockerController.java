package com.campushub.modules.locker.controller;

import com.campushub.modules.locker.dto.*;
import com.campushub.modules.locker.entity.LockerStatus;
import com.campushub.modules.locker.service.LockerService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/lockers")
public class LockerController {

    private final LockerService lockerService;

    public LockerController(LockerService lockerService) {
        this.lockerService = lockerService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<LockerResponse>> getLockers(
            @RequestParam(required = false) LockerStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction) {

        if (size > 50) size = 50;
        Sort.Direction dir = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortProp = sort.equals("code") ? "code" : (sort.equals("name") ? "name" : "createdAt");
        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, sortProp));

        return ResponseEntity.ok(lockerService.getLockers(status, search, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LockerDetailResponse> getLocker(@PathVariable Long id) {
        return ResponseEntity.ok(lockerService.getLocker(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LockerResponse> createLocker(@Valid @RequestBody LockerCreateRequest request) {
        LockerResponse response = lockerService.createLocker(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LockerResponse> updateLocker(
            @PathVariable Long id,
            @Valid @RequestBody LockerUpdateRequest request) {
        return ResponseEntity.ok(lockerService.updateLocker(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LockerResponse> updateLockerStatus(
            @PathVariable Long id,
            @Valid @RequestBody LockerStatusUpdateRequest request) {
        return ResponseEntity.ok(lockerService.updateLockerStatus(id, request));
    }

    @GetMapping("/{lockerId}/compartments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CompartmentResponse>> getCompartments(@PathVariable Long lockerId) {
        return ResponseEntity.ok(lockerService.getCompartments(lockerId));
    }

    @PostMapping("/{lockerId}/compartments")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CompartmentResponse> createCompartment(
            @PathVariable Long lockerId,
            @Valid @RequestBody CompartmentCreateRequest request) {
        CompartmentResponse response = lockerService.createCompartment(lockerId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{lockerId}/compartments/{compartmentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CompartmentResponse> getCompartment(
            @PathVariable Long lockerId,
            @PathVariable Long compartmentId) {
        return ResponseEntity.ok(lockerService.getCompartment(lockerId, compartmentId));
    }

    @PatchMapping("/{lockerId}/compartments/{compartmentId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CompartmentResponse> updateCompartmentStatus(
            @PathVariable Long lockerId,
            @PathVariable Long compartmentId,
            @Valid @RequestBody CompartmentStatusUpdateRequest request) {
        return ResponseEntity.ok(lockerService.updateCompartmentStatus(lockerId, compartmentId, request));
    }
}
