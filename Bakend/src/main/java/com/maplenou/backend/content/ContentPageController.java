package com.maplenou.backend.content;

import com.maplenou.backend.content.dto.ContentPageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Pages statiques publiques (CGU, CGV, FAQ, à propos…). */
@RestController
@RequiredArgsConstructor
@Tag(name = "Pages de contenu")
@RequestMapping("/api/pages")
public class ContentPageController {

    private final ContentPageService contentPageService;

    @GetMapping("/{slug}")
    public ContentPageResponse getBySlug(@PathVariable String slug) {
        return contentPageService.getPublishedBySlug(slug);
    }
}
