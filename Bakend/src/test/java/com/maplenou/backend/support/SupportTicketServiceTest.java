package com.maplenou.backend.support;

import com.maplenou.backend.audit.AuditService;
import com.maplenou.backend.common.exception.ApiException;
import com.maplenou.backend.delivery.DeliveryRepository;
import com.maplenou.backend.messaging.ConversationRepository;
import com.maplenou.backend.messaging.MessagingService;
import com.maplenou.backend.support.dto.ResolveSupportTicketRequest;
import com.maplenou.backend.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/** Passage au service client : l'ancienne page admin passe en consultation seule (SUPPORT_LEGACY_READ_ONLY). */
class SupportTicketServiceTest {

    @Test
    void resolveIsRefusedOnceTheTicketsAreHandledInTheCustomerService() {
        SupportTicketRepository repository = mock(SupportTicketRepository.class);
        AuditService audit = mock(AuditService.class);
        SupportTicketService service = new SupportTicketService(repository, mock(DeliveryRepository.class),
                mock(ConversationRepository.class), mock(MessagingService.class), audit);
        ReflectionTestUtils.setField(service, "legacyReadOnly", true);

        assertThatThrownBy(() -> service.resolve(UUID.randomUUID(),
                new ResolveSupportTicketRequest(SupportTicketStatus.RESOLVED, "ok"), mock(User.class), "127.0.0.1"))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> org.assertj.core.api.Assertions.assertThat(((ApiException) e).getStatus())
                        .isEqualTo(HttpStatus.CONFLICT));
        verify(repository, never()).save(any());
        verifyNoInteractions(audit);
    }
}
