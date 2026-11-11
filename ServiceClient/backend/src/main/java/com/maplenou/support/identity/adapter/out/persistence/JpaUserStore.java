package com.maplenou.support.identity.adapter.out.persistence;

import com.maplenou.support.identity.application.port.UserStore;
import com.maplenou.support.identity.domain.SupportUser;
import com.maplenou.support.shared.Role;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class JpaUserStore implements UserStore {

    private final SupportUserJpaRepository repo;

    JpaUserStore(SupportUserJpaRepository repo) {
        this.repo = repo;
    }

    @Override
    public Optional<SupportUser> find(UUID id) {
        return repo.findById(id).map(SupportUserEntity::toDomain);
    }

    @Override
    public List<SupportUser> findAll(Collection<UUID> ids) {
        return repo.findAllById(ids).stream().map(SupportUserEntity::toDomain).toList();
    }

    @Override
    public List<SupportUser> withRoles(Collection<Role> roles, boolean activeOnly) {
        return (activeOnly ? repo.findByRoleInAndActiveTrueOrderByDisplayNameAsc(roles)
                : repo.findByRoleInOrderByDisplayNameAsc(roles)).stream().map(SupportUserEntity::toDomain).toList();
    }

    @Override
    public void save(SupportUser user) {
        SupportUserEntity e = repo.findById(user.id()).orElseGet(() -> new SupportUserEntity(user.id()));
        e.copyFrom(user);
        repo.save(e);
    }
}
