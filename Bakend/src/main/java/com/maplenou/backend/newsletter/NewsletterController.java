package com.maplenou.backend.newsletter;

import com.maplenou.backend.newsletter.dto.SubscribeRequest;
import com.maplenou.backend.newsletter.dto.SubscriberResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Newsletter")
@RequestMapping("/api/newsletter")
public class NewsletterController {

    private final NewsletterService newsletterService;

    @PostMapping("/subscribe")
    public ResponseEntity<SubscriberResponse> subscribe(@Valid @RequestBody SubscribeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(newsletterService.subscribe(request.email()));
    }

    @PostMapping("/unsubscribe")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsubscribe(@Valid @RequestBody SubscribeRequest request) {
        newsletterService.unsubscribe(request.email());
    }
}
