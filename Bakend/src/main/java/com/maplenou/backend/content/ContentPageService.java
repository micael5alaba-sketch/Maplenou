package com.maplenou.backend.content;

import com.maplenou.backend.audit.AuditAction;
import com.maplenou.backend.audit.AuditService;
import com.maplenou.backend.common.exception.ApiException;
import com.maplenou.backend.content.dto.ContentPageResponse;
import com.maplenou.backend.content.dto.CreateContentPageRequest;
import com.maplenou.backend.content.dto.UpdateContentPageRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContentPageService {

    private final ContentPageRepository contentPageRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public ContentPageResponse getPublishedBySlug(String slug) {
        return contentPageRepository.findBySlugAndPublishedTrue(slug)
                .map(ContentPageResponse::from)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Page introuvable"));
    }

    @Transactional(readOnly = true)
    public Page<ContentPageResponse> listAll(Pageable pageable) {
        return contentPageRepository.findAll(pageable).map(ContentPageResponse::from);
    }

    @Transactional(readOnly = true)
    public ContentPageResponse getById(UUID id) {
        return ContentPageResponse.from(getEntityById(id));
    }

    @Transactional
    public ContentPageResponse create(CreateContentPageRequest request, UUID actorId, String ip) {
        if (contentPageRepository.existsBySlug(request.slug())) {
            throw new ApiException(HttpStatus.CONFLICT, "Une page avec ce slug existe déjà");
        }
        ContentPage page = ContentPage.builder()
                .slug(request.slug())
                .title(request.title())
                .body(request.body())
                .published(request.published())
                .build();
        contentPageRepository.save(page);
        auditService.log(actorId, AuditAction.CONTENT_PAGE_CREATED, "CONTENT_PAGE", page.getId(),
                "Page créée : " + page.getSlug(), ip);
        return ContentPageResponse.from(page);
    }

    @Transactional
    public ContentPageResponse update(UUID id, UpdateContentPageRequest request, UUID actorId, String ip) {
        ContentPage page = getEntityById(id);
        page.setTitle(request.title());
        page.setBody(request.body());
        page.setPublished(request.published());
        contentPageRepository.save(page);
        auditService.log(actorId, AuditAction.CONTENT_PAGE_UPDATED, "CONTENT_PAGE", page.getId(),
                "Page mise à jour : " + page.getSlug(), ip);
        return ContentPageResponse.from(page);
    }

    @Transactional
    public void delete(UUID id, UUID actorId, String ip) {
        ContentPage page = getEntityById(id);
        contentPageRepository.delete(page);
        auditService.log(actorId, AuditAction.CONTENT_PAGE_DELETED, "CONTENT_PAGE", id,
                "Page supprimée : " + page.getSlug(), ip);
    }

    private ContentPage getEntityById(UUID id) {
        return contentPageRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Page introuvable"));
    }
}
