package com.campushub.common.enums;

/**
 * User roles in the system.
 * Each role carries a Spring Security authority string prefixed with "ROLE_".
 */
public enum Role {
    STUDENT,
    ADMIN;

    /**
     * Returns the Spring Security authority string for this role (e.g. "ROLE_ADMIN").
     */
    public String toAuthority() {
        return "ROLE_" + this.name();
    }
}
