package com.maplenou.support.ticket.adapter.out.persistence;

import com.maplenou.support.ticket.application.port.LegacySyncStore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

interface LegacySyncJpaRepository extends JpaRepository<LegacySyncEntity, UUID> {
}

@Component
class JpaLegacySyncStore implements LegacySyncStore {

    private final LegacySyncJpaRepository repo;

    JpaLegacySyncStore(LegacySyncJpaRepository repo) {
        this.repo = repo;
    }

    @Override
    public Map<UUID, LegacyLink> find(Collection<UUID> legacyIds) {
        if (legacyIds.isEmpty()) {
            return Map.of();
        }
        return repo.findAllById(legacyIds).stream().map(LegacySyncEntity::toDomain)
                .collect(Collectors.toMap(LegacyLink::legacyId, Function.identity()));
    }

    @Override
    public void save(LegacyLink link) {
        repo.save(new LegacySyncEntity(link));
    }
}
