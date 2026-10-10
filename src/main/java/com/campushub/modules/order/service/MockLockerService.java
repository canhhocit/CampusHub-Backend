package com.campushub.modules.order.service;

import com.campushub.common.exception.AppException;
import com.campushub.common.exception.ErrorCode;
import com.campushub.modules.locker.entity.Compartment;
import com.campushub.modules.locker.entity.CompartmentStatus;
import com.campushub.modules.locker.entity.Locker;
import com.campushub.modules.locker.entity.LockerStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MockLockerService {

    private static final Logger log = LoggerFactory.getLogger(MockLockerService.class);

    /**
     * Simulate opening locker compartment for seller to place the item.
     */
    public boolean openForDeposit(Locker locker, Compartment compartment) {
        log.info("[MOCK IoT] Request to open compartment {} on locker {} for deposit", compartment.getCode(), locker.getCode());
        validateDeviceHealth(locker, compartment);
        log.info("[MOCK IoT] Door OPENED for deposit: Locker={}, Compartment={}", locker.getCode(), compartment.getCode());
        return true;
    }

    /**
     * Simulate door closing and sensor confirming item presence.
     */
    public boolean confirmItemDeposited(Locker locker, Compartment compartment) {
        log.info("[MOCK IoT] Sensor check: item detected inside compartment {} of locker {}", compartment.getCode(), locker.getCode());
        validateDeviceHealth(locker, compartment);
        log.info("[MOCK IoT] Door CLOSED and LOCKED. Deposit confirmed.");
        return true;
    }

    /**
     * Simulate opening locker compartment for buyer upon valid pickup code entry.
     */
    public boolean openForPickup(Locker locker, Compartment compartment) {
        log.info("[MOCK IoT] Request to open compartment {} on locker {} for pickup", compartment.getCode(), locker.getCode());
        validateDeviceHealth(locker, compartment);
        log.info("[MOCK IoT] Door OPENED for pickup: Locker={}, Compartment={}", locker.getCode(), compartment.getCode());
        return true;
    }

    /**
     * Simulate door closing and sensor confirming item was retrieved.
     */
    public boolean confirmItemPickedUp(Locker locker, Compartment compartment) {
        log.info("[MOCK IoT] Sensor check: compartment {} of locker {} is now empty", compartment.getCode(), locker.getCode());
        validateDeviceHealth(locker, compartment);
        log.info("[MOCK IoT] Door CLOSED. Compartment is now clear.");
        return true;
    }

    private void validateDeviceHealth(Locker locker, Compartment compartment) {
        if (locker.getStatus() != LockerStatus.ACTIVE) {
            log.error("[MOCK IoT] Hardware error: Locker {} status is {}", locker.getCode(), locker.getStatus());
            throw new AppException(ErrorCode.LOCKER_NOT_ACTIVE);
        }
        if (compartment.getStatus() == CompartmentStatus.ERROR || compartment.getStatus() == CompartmentStatus.OUT_OF_SERVICE) {
            log.error("[MOCK IoT] Hardware error: Compartment {} status is {}", compartment.getCode(), compartment.getStatus());
            throw new AppException(ErrorCode.MOCK_IOT_DEVICE_ERROR);
        }
    }
}
