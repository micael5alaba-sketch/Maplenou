package com.maplenou.support.ticket.adapter.out.assistant;

import com.maplenou.support.ticket.application.port.AiAssistant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Calls the documentation assistant ({@code assistant/}: {@code POST /api/assistant/internal/triage} and
 * {@code /draft-reply}) with the shared {@code X-Assistant-Token}. These endpoints are internal: the docs web server
 * never exposes them.
 */
public class HttpAiAssistant implements AiAssistant {

    private static final Logger log = LoggerFactory.getLogger(HttpAiAssistant.class);

    record TriageResponse(String categoryCode, String priorityCode, Double confidence, String reason, String model) {
    }

    record DraftResponse(String text, List<String> references, String model) {
    }

    private final RestClient client;

    public HttpAiAssistant(RestClient client) {
        this.client = client;
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public Optional<Triage> triage(String subject, String description, String language, List<Option> categories,
                                   List<Option> priorities) {
        try {
            TriageResponse r = client.post().uri("/api/assistant/internal/triage")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("subject", subject, "description", description, "language", language,
                            "categories", categories, "priorities", priorities))
                    .retrieve().body(TriageResponse.class);
            if (r == null || r.categoryCode() == null || r.priorityCode() == null) {
                return Optional.empty();
            }
            return Optional.of(new Triage(r.categoryCode(), r.priorityCode(),
                    r.confidence() == null ? 0.5 : r.confidence(), r.reason(), r.model()));
        } catch (RestClientException e) {
            log.warn("Assistant triage unavailable: {}", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<Draft> draftReply(String subject, String description, List<ConversationLine> conversation,
                                      String language, String agentName) {
        try {
            DraftResponse r = client.post().uri("/api/assistant/internal/draft-reply")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("subject", subject, "description", description, "conversation", conversation,
                            "language", language, "agentName", agentName == null ? "" : agentName))
                    .retrieve().body(DraftResponse.class);
            return r == null || r.text() == null || r.text().isBlank() ? Optional.empty()
                    : Optional.of(new Draft(r.text(), r.references() == null ? List.of() : r.references(), r.model()));
        } catch (RestClientException e) {
            log.warn("Assistant draft unavailable: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
