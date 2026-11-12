package com.maplenou.support.identity.adapter.out.persistence;

import com.maplenou.support.shared.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

interface SupportUserJpaRepository extends JpaRepository<SupportUserEntity, UUID> {

    List<SupportUserEntity> findByRoleInOrderByDisplayNameAsc(Collection<Role> roles);

    List<SupportUserEntity> findByRoleInAndActiveTrueOrderByDisplayNameAsc(Collection<Role> roles);
}
