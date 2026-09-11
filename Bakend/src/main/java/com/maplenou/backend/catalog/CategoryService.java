package com.maplenou.backend.catalog;

import com.maplenou.backend.catalog.dto.CreateCategoryRequest;
import com.maplenou.backend.catalog.dto.UpdateCategoryRequest;
import com.maplenou.backend.common.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");

    // ----- Lecture (public) -----

    @Cacheable("categories")
    public List<Category> getRootCategories() {
        return categoryRepository.findByParentIsNullAndActiveTrueOrderByNameAsc();
    }

    @Cacheable(value = "categories", key = "#parentId")
    public List<Category> getSubCategories(UUID parentId) {
        return categoryRepository.findByParentIdAndActiveTrueOrderByNameAsc(parentId);
    }

    public Category getBySlug(String slug) {
        return categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Catégorie introuvable"));
    }

    public Category getById(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Catégorie introuvable"));
    }

    // ----- Écriture (admin uniquement, vérifié au niveau du contrôleur) -----

    @CacheEvict(value = "categories", allEntries = true)
    @Transactional
    public Category create(CreateCategoryRequest request) {
        String slug = (request.slug() != null && !request.slug().isBlank())
                ? slugify(request.slug())
                : slugify(request.name());

        if (categoryRepository.existsBySlug(slug)) {
            throw new ApiException(HttpStatus.CONFLICT, "Une catégorie avec ce slug existe déjà");
        }

        Category parent = null;
        if (request.parentId() != null) {
            parent = getById(request.parentId());
        }

        Category category = Category.builder()
                .name(request.name())
                .slug(slug)
                .parent(parent)
                .imageUrl(request.imageUrl())
                .active(true)
                .build();

        return categoryRepository.save(category);
    }

    @CacheEvict(value = "categories", allEntries = true)
    @Transactional
    public Category update(UUID id, UpdateCategoryRequest request) {
        Category category = getById(id);

        if (request.name() != null && !request.name().isBlank()) {
            category.setName(request.name());
        }
        if (request.active() != null) {
            category.setActive(request.active());
        }
        if (request.parentId() != null) {
            if (request.parentId().equals(id)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Une catégorie ne peut pas être son propre parent");
            }
            category.setParent(getById(request.parentId()));
        }
        if (request.imageUrl() != null) {
            category.setImageUrl(request.imageUrl().isBlank() ? null : request.imageUrl());
        }

        return categoryRepository.save(category);
    }

    @CacheEvict(value = "categories", allEntries = true)
    @Transactional
    public void deactivate(UUID id) {
        Category category = getById(id);
        category.setActive(false);
        categoryRepository.save(category);
    }

    private String slugify(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .trim();
        String slug = NON_ALPHANUMERIC.matcher(normalized).replaceAll("-");
        return slug.replaceAll("^-+|-+$", "");
    }
}
