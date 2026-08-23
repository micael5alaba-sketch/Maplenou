package com.maplenou.backend.catalog;

import com.maplenou.backend.catalog.dto.CategoryResponse;
import com.maplenou.backend.catalog.dto.CreateCategoryRequest;
import com.maplenou.backend.catalog.dto.UpdateCategoryRequest;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Catégories")
public class CategoryController {

    private final CategoryService categoryService;

    // ----- Public : consultation du catalogue -----

    @GetMapping
    public List<CategoryResponse> getRootCategories() {
        return categoryService.getRootCategories().stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @GetMapping("/{parentId}/subcategories")
    public List<CategoryResponse> getSubCategories(@PathVariable UUID parentId) {
        return categoryService.getSubCategories(parentId).stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @GetMapping("/slug/{slug}")
    public CategoryResponse getBySlug(@PathVariable String slug) {
        return CategoryResponse.from(categoryService.getBySlug(slug));
    }

    // ----- Admin uniquement : gestion du référentiel -----
    // @PreAuthorize vérifie le rôle à l'entrée de la méthode, avant même d'atteindre le service.
    // C'est la même annotation qu'on réutilisera pour Shop/Product avec des règles plus fines.

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CreateCategoryRequest request) {
        Category created = categoryService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(CategoryResponse.from(created));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public CategoryResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateCategoryRequest request) {
        return CategoryResponse.from(categoryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable UUID id) {
        categoryService.deactivate(id);
    }
}
