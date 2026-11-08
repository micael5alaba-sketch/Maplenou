package com.maplenou.support.identity.adapter.in.web;

import com.maplenou.support.identity.application.UserDirectory;
import com.maplenou.support.platform.security.CurrentActorProvider;
import com.maplenou.support.shared.Actor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Me")
public class MeController {

    private final CurrentActorProvider currentUser;
    private final UserDirectory directory;

    public MeController(CurrentActorProvider currentUser, UserDirectory directory) {
        this.currentUser = currentUser;
        this.directory = directory;
    }

    @GetMapping("/api/v1/support/me")
    public Actor me() {
        return currentUser.get();
    }

    public record Preferences(@Pattern(regexp = "^(fr|en)$") String language) {
    }

    @Operation(summary = "Language of the notifications the caller receives (fr or en)")
    @PutMapping("/api/v1/support/me/preferences")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void preferences(@Valid @RequestBody Preferences body) {
        if (body.language() != null) {
            directory.setLanguage(currentUser.get(), body.language());
        }
    }
}
