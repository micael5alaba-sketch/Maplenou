package com.maplenou.support.identity.domain;

import com.maplenou.support.shared.Role;

import java.time.Instant;
import java.util.UUID;

/** Support role given to a Maplenou account, by phone number (DEC-CS10-17). Applied at the next sign-in. */
public record StaffAssignment(String phone, Role role, UUID assignedBy, Instant assignedAt) {
}
