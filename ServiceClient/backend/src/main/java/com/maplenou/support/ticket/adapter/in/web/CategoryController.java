package com.maplenou.support.ticket.adapter.in.web;

import com.maplenou.support.platform.security.CurrentActorProvider;
import com.maplenou.support.ticket.application.CategoryService;
import com.maplenou.support.ticket.application.view.TicketViews.CategoryView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

@RestController
@Tag(name = "Categories")
public class CategoryController {

    private final CategoryService categories;
    private final CurrentActorProvider currentUser;

    public CategoryController(CategoryService categories, CurrentActorProvider currentUser) {
        this.categories = categories;
        this.currentUser = currentUser;
    }

    /**
     * Active categories for the create-ticket form (BR-006). Served from the reference cache; the browser may
     * reuse the response for 5 minutes and then revalidates it with its ETag (304 Not Modified, no body).
     */
    @GetMapping("/api/v1/support/categories")
    public ResponseEntity<List<CategoryView>> list() {
        List<CategoryView> body = categories.active().stream().map(CategoryView::of).toList();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePrivate())
                .eTag(Integer.toHexString(body.hashCode()))
                .body(body);
    }

    @GetMapping("/api/v1/support/admin/categories")
    @PreAuthorize("hasAnyRole('SUPPORT_MANAGER','ADMINISTRATOR')")
    public List<CategoryView> all() {
        return categories.all().stream().map(CategoryView::of).toList();
    }

    public record CategoryCreate(@NotBlank @Size(max = 30) String code, @NotBlank @Size(max = 80) String name,
                                 @Size(max = 80) String nameEn, @Size(max = 255) String description,
                                 @Size(max = 255) String descriptionEn) {
    }

    public record CategoryUpdate(@NotBlank @Size(max = 80) String name, @Size(max = 80) String nameEn,
                                 @Size(max = 255) String description, @Size(max = 255) String descriptionEn,
                                 Integer sortOrder, boolean active) {
    }

    @Operation(summary = "Add a category (administrators, DEC-CS11-06)")
    @PostMapping("/api/v1/support/admin/categories")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryView create(@Valid @RequestBody CategoryCreate body) {
        return CategoryView.of(categories.create(currentUser.get(), body.code(), body.name(), body.nameEn(),
                body.description(), body.descriptionEn()));
    }

    /** Rename / describe / reorder / (de)activate. Categories are never deleted (tickets keep them). */
    @PutMapping("/api/v1/support/admin/categories/{code}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public CategoryView update(@PathVariable String code, @Valid @RequestBody CategoryUpdate body) {
        return CategoryView.of(categories.update(currentUser.get(), code, body.name(), body.nameEn(), body.description(),
                body.descriptionEn(), body.sortOrder(), body.active()));
    }
}
