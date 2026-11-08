package com.maplenou.support.identity.application.port;

import com.maplenou.support.identity.domain.SupportUser;
import com.maplenou.support.shared.Role;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Local user projections (table {@code support_users}). */
public interface UserStore {

    Optional<SupportUser> find(UUID id);

    List<SupportUser> findAll(Collection<UUID> ids);

    /** Users with one of the roles, by name. */
    List<SupportUser> withRoles(Collection<Role> roles, boolean activeOnly);

    void save(SupportUser user);
}
