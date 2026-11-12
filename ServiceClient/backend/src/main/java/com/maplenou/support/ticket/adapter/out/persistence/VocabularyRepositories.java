package com.maplenou.support.ticket.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface CategoryJpaRepository extends JpaRepository<CategoryEntity, UUID> {
    Optional<CategoryEntity> findByCode(String code);
}

interface StatusJpaRepository extends JpaRepository<StatusEntity, String> {
}

interface TransitionJpaRepository extends JpaRepository<TransitionEntity, TransitionId> {
}

interface PriorityJpaRepository extends JpaRepository<PriorityEntity, String> {
}

interface PermissionRuleJpaRepository extends JpaRepository<PermissionRuleEntity, PermissionRuleId> {
}
