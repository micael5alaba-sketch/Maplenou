package com.maplenou.support.identity.application.port;

import com.maplenou.support.identity.domain.StaffAssignment;

import java.util.List;
import java.util.Optional;

/** Support roles given by phone number (table {@code support_staff_assignments}). */
public interface StaffAssignmentStore {

    List<StaffAssignment> all();

    Optional<StaffAssignment> find(String phone);

    void save(StaffAssignment assignment);

    void delete(String phone);
}
