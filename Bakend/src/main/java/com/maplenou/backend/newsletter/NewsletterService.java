package com.maplenou.backend.newsletter;

import com.maplenou.backend.common.exception.ApiException;
import com.maplenou.backend.newsletter.dto.SubscriberResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Capture des abonnements newsletter. L'envoi effectif des campagnes est délégué à l'outil
 * d'emailing choisi par l'équipe marketing (Mailchimp, Brevo…) — hors périmètre backend
 * jusqu'à décision de l'outil (§3.5 : "intégration avec l'outil d'emailing existant").
 */
@Service
@RequiredArgsConstructor
public class NewsletterService {

    private final NewsletterSubscriberRepository subscriberRepository;

    @Transactional
    public SubscriberResponse subscribe(String email) {
        NewsletterSubscriber subscriber = subscriberRepository.findByEmail(email)
                .orElseGet(() -> NewsletterSubscriber.builder().email(email).build());

        if (subscriber.getId() != null && subscriber.isActive()) {
            throw new ApiException(HttpStatus.CONFLICT, "Cet email est déjà abonné à la newsletter");
        }

        subscriber.setActive(true);
        subscriber.setUnsubscribedAt(null);
        return SubscriberResponse.from(subscriberRepository.save(subscriber));
    }

    @Transactional
    public void unsubscribe(String email) {
        NewsletterSubscriber subscriber = subscriberRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Abonné introuvable"));
        subscriber.setActive(false);
        subscriber.setUnsubscribedAt(Instant.now());
        subscriberRepository.save(subscriber);
    }

    @Transactional(readOnly = true)
    public Page<SubscriberResponse> listActive(Pageable pageable) {
        return subscriberRepository.findByActiveTrue(pageable).map(SubscriberResponse::from);
    }
}
