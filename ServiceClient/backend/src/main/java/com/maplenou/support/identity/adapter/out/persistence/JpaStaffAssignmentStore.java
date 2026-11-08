package com.maplenou.support.identity.adapter.out.persistence;

import com.maplenou.support.identity.application.port.StaffAssignmentStore;
import com.maplenou.support.identity.domain.StaffAssignment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
class JpaStaffAssignmentStore implements StaffAssignmentStore {

    private final StaffAssignmentJpaRepository repo;

    JpaStaffAssignmentStore(StaffAssignmentJpaRepository repo) {
        this.repo = repo;
    }

    @Override
    public List<StaffAssignment> all() {
        return repo.findAll().stream().map(StaffAssignmentEntity::toDomain).toList();
    }

    @Override
    public Optional<StaffAssignment> find(String phone) {
        return repo.findById(phone).map(StaffAssignmentEntity::toDomain);
    }

    @Override
    public void save(StaffAssignment a) {
        StaffAssignmentEntity e = repo.findById(a.phone()).orElseGet(() -> new StaffAssignmentEntity(a));
        e.copyFrom(a);
        repo.save(e);
    }

    @Override
    public void delete(String phone) {
        repo.deleteById(phone);
    }
}
