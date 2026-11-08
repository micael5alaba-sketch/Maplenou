package com.maplenou.support.identity.adapter.out.persistence;

import com.maplenou.support.identity.domain.SupportUser;
import com.maplenou.support.platform.persistence.BaseEntity;
import com.maplenou.support.shared.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "support_users")
class SupportUserEntity extends BaseEntity {

    @Column(name = "display_name", nullable = false)
    private String displayName;
    private String email;
    private String phone;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
    @Column(nullable = false)
    private boolean active = true;
    @Column(nullable = false)
    private String language = "fr";
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SupportUserEntity() {
    }

    SupportUserEntity(UUID id) {
        super(id);
    }

    void copyFrom(SupportUser u) {
        this.displayName = u.displayName();
        this.email = u.email();
        this.phone = u.phone();
        this.role = u.role();
        this.active = u.active();
        this.language = u.language();
        this.createdAt = u.createdAt();
        this.updatedAt = u.updatedAt();
    }

    SupportUser toDomain() {
        return new SupportUser(getId(), displayName, email, phone, role, active, language, createdAt, updatedAt);
    }
}
