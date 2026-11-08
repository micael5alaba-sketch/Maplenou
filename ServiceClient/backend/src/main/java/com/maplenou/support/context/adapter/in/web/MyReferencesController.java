package com.maplenou.support.context.adapter.in.web;

import com.maplenou.support.context.application.ReferenceService;
import com.maplenou.support.platform.security.CurrentActorProvider;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Ecosystem context")
public class MyReferencesController {

    private final ReferenceService references;
    private final CurrentActorProvider currentUser;

    public MyReferencesController(ReferenceService references, CurrentActorProvider currentUser) {
        this.references = references;
        this.currentUser = currentUser;
    }

    /** The caller's own orders (customer/seller) or deliveries (driver), for the create-ticket form. */
    @GetMapping("/api/v1/support/me/references")
    public ReferenceService.MyReferences myReferences() {
        return references.myReferences(currentUser.get());
    }
}
