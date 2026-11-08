package com.maplenou.support.identity.adapter.out.persistence;

import com.maplenou.support.identity.domain.StaffAssignment;
import com.maplenou.support.shared.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "support_staff_assignments")
class StaffAssignmentEntity {

    @Id
    private String phone;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
    @Column(name = "assigned_by")
    private UUID assignedBy;
    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    protected StaffAssignmentEntity() {
    }

    StaffAssignmentEntity(StaffAssignment a) {
        this.phone = a.phone();
        copyFrom(a);
    }

    void copyFrom(StaffAssignment a) {
        this.role = a.role();
        this.assignedBy = a.assignedBy();
        this.assignedAt = a.assignedAt();
    }

    StaffAssignment toDomain() {
        return new StaffAssignment(phone, role, assignedBy, assignedAt);
    }
}
