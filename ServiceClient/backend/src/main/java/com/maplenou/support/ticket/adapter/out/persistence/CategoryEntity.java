package com.maplenou.support.ticket.adapter.out.persistence;

import com.maplenou.support.platform.persistence.BaseEntity;
import com.maplenou.support.ticket.domain.Category;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "ticket_categories")
class CategoryEntity extends BaseEntity {

    @Column(nullable = false, unique = true, updatable = false)
    private String code;
    @Column(nullable = false)
    private String name;
    @Column(name = "name_en")
    private String nameEn;
    private String description;
    @Column(name = "description_en")
    private String descriptionEn;
    @Column(name = "sort_order", nullable = false)
    private short sortOrder;
    @Column(nullable = false)
    private boolean active;

    protected CategoryEntity() {
    }

    CategoryEntity(UUID id, String code) {
        super(id);
        this.code = code;
    }

    void copyFrom(Category c) {
        this.name = c.name();
        this.nameEn = c.nameEn();
        this.description = c.description();
        this.descriptionEn = c.descriptionEn();
        this.sortOrder = (short) c.sortOrder();
        this.active = c.active();
    }

    Category toDomain() {
        return new Category(getId(), code, name, nameEn, description, descriptionEn, sortOrder, active);
    }

    String getCode() {
        return code;
    }
}
