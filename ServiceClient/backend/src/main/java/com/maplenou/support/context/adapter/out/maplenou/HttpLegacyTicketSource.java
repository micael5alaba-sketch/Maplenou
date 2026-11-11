package com.maplenou.support.context.adapter.out.maplenou;

import com.maplenou.support.context.application.port.LegacyTicketSource;
import com.maplenou.support.platform.config.MaplenouProperties;
import com.maplenou.support.shared.SupportException;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

/** Bakend {@code GET /internal/v1/support-tickets}; needs MAPLENOU_CORE_URL and SUPPORT_SERVICE_TOKEN. */
public class HttpLegacyTicketSource implements LegacyTicketSource {

    private final MaplenouProperties props;
    private final RestClient.Builder builder;

    public HttpLegacyTicketSource(MaplenouProperties props, RestClient.Builder builder) {
        this.props = props;
        this.builder = builder;
    }

    @Override
    public LegacyTicketPage page(int page, int size) {
        String token = props.ecosystem() == null ? null : props.ecosystem().serviceToken();
        String baseUrl = props.core() == null ? null : props.core().baseUrl();
        if (token == null || token.isBlank() || baseUrl == null || baseUrl.isBlank()) {
            throw SupportException.businessRule("LEGACY_IMPORT_NOT_CONFIGURED",
                    "Import impossible : renseignez MAPLENOU_CORE_URL et SUPPORT_SERVICE_TOKEN (des deux côtés).", null);
        }
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(props.core().timeout());
        rf.setReadTimeout(props.core().timeout());
        try {
            LegacyTicketPage body = builder.clone().baseUrl(baseUrl).requestFactory(rf)
                    .defaultHeader(BakendClient.SERVICE_TOKEN_HEADER, token).build()
                    .get().uri("/internal/v1/support-tickets?page={p}&size={s}", page, size)
                    .retrieve().body(LegacyTicketPage.class);
            return body == null ? new LegacyTicketPage(List.of(), page, size, 0, true) : body;
        } catch (RestClientException e) {
            throw SupportException.unavailable("ECOSYSTEM_UNAVAILABLE",
                    "La plateforme Maplenou ne répond pas (vérifiez MAPLENOU_CORE_URL et SUPPORT_SERVICE_TOKEN).");
        }
    }
}
