package com.maplenou.support.it;

import com.maplenou.support.notification.application.PushDispatcher;
import com.maplenou.support.notification.application.port.NotificationStore;
import com.maplenou.support.notification.application.port.PushSender;
import com.maplenou.support.notification.domain.Notification;
import com.maplenou.support.shared.Actor;
import com.maplenou.support.shared.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** FR-013, BR-020 — AC-011: notification failure must not corrupt ticket state. */
@TestPropertySource(properties = "maplenou.notifications.max-attempts=2")
class NotificationIT extends IntegrationTest {

    @MockitoBean
    PushSender pushSender;

    @Autowired
    PushDispatcher dispatcher;

    @Autowired
    NotificationStore notifications;

    /** Processes batches until nothing is due (the shared test database holds other tests' notifications). */
    private void drainPushQueue() {
        for (int i = 0; i < 1000 && dispatcher.dispatchDue() > 0; i++) {
            // keep going
        }
    }

    @Test
    void requesterIsNotifiedOfReplyStatusChangeAndResolution() throws Exception {
        Actor customer = newUser(Role.CUSTOMER);
        Actor agent = newUser(Role.SUPPORT_AGENT);
        String id = createTicket(customer);
        assignSelf(agent, id);
        reply(agent, id, "Pouvez-vous préciser ?");
        setStatus(agent, id, "WAITING_FOR_CUSTOMER");
        reply(customer, id, "Voici");
        setStatus(agent, id, "RESOLVED");

        List<String> types = json(getAs(customer, "/api/v1/support/notifications?size=50"), "$.items[*].type");
        assertThat(types).contains("TICKET_CREATED", "AGENT_REPLIED", "INFORMATION_REQUESTED", "TICKET_RESOLVED");
        List<String> agentTypes = json(getAs(agent, "/api/v1/support/notifications?size=50"), "$.items[*].type");
        assertThat(agentTypes).contains("REQUESTER_REPLIED");
        // users are not notified about their own actions
        assertThat(agentTypes).doesNotContain("TICKET_ASSIGNED");

        getAs(customer, "/api/v1/support/notifications/unread-count").andExpect(jsonPath("$.unread").value(
                org.hamcrest.Matchers.greaterThanOrEqualTo(4)));
        postJson(customer, "/api/v1/support/notifications/read-all", "").andExpect(status().isOk());
        getAs(customer, "/api/v1/support/notifications/unread-count").andExpect(jsonPath("$.unread").value(0));
    }

    @Test
    void ac011_pushFailureDoesNotAffectTicketAndIsRetriedThenMarkedFailed() throws Exception {
        reset(pushSender);
        when(pushSender.send(any())).thenThrow(new PushSender.PushDeliveryException("FCM down", null));
        Actor customer = newUser(Role.CUSTOMER);
        Actor agent = newUser(Role.SUPPORT_AGENT);
        postJson(customer, "/api/v1/support/notifications/devices",
                "{\"token\":\"tok-" + UUID.randomUUID() + "\",\"platform\":\"ANDROID\"}").andExpect(status().isNoContent());
        String id = createTicket(customer);
        assignSelf(agent, id);
        reply(agent, id, "Réponse");

        drainPushQueue();

        // ticket state is intact
        getAs(customer, "/api/v1/support/tickets/{id}", id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        List<Notification> forTicket = notifications.forTicket(UUID.fromString(id)).stream()
                .filter(n -> n.recipientId().equals(customer.id())).toList();
        assertThat(forTicket).isNotEmpty();
        assertThat(forTicket).allSatisfy(n -> {
            // language-neutral facts for clients that render the notification in English (V2)
            assertThat(n.ticketNumber()).startsWith("TKT-");
            assertThat(n.pushStatus()).isEqualTo(Notification.PushStatus.PENDING);
            assertThat(n.pushAttempts()).isEqualTo(1);
            assertThat(n.pushLastError()).contains("FCM down");
        });
        // the in-app notification is still available to the user
        List<String> types = json(getAs(customer, "/api/v1/support/notifications?size=50"), "$.items[*].type");
        assertThat(types).contains("AGENT_REPLIED");
        List<String> numbers = json(getAs(customer, "/api/v1/support/notifications?size=50"), "$.items[*].ticketNumber");
        assertThat(numbers).doesNotContainNull();
    }

    @Test
    void pushIsSkippedWhenUserHasNoDevice() throws Exception {
        reset(pushSender);
        Actor customer = newUser(Role.CUSTOMER);
        String id = createTicket(customer);
        drainPushQueue();
        List<Notification> list = notifications.forTicket(UUID.fromString(id)).stream()
                .filter(n -> n.recipientId().equals(customer.id())).toList();
        assertThat(list).extracting(Notification::pushStatus).containsOnly(Notification.PushStatus.SKIPPED);
    }

    @Test
    void pushIsSentToRegisteredDevices() throws Exception {
        reset(pushSender);
        when(pushSender.send(any())).thenReturn(List.of());
        Actor customer = newUser(Role.CUSTOMER);
        postJson(customer, "/api/v1/support/notifications/devices",
                "{\"token\":\"tok-" + UUID.randomUUID() + "\",\"platform\":\"IOS\"}").andExpect(status().isNoContent());
        String id = createTicket(customer);
        drainPushQueue();
        List<Notification> list = notifications.forTicket(UUID.fromString(id)).stream()
                .filter(n -> n.recipientId().equals(customer.id())).toList();
        assertThat(list).extracting(Notification::pushStatus).containsOnly(Notification.PushStatus.SENT);
        // the mobile app opens the ticket from the push (DEC-CS11-09): generic text, a link and the source
        verify(pushSender).send(argThat(m -> "support".equals(m.data().get("source"))
                && ("http://localhost:3001/?ticket=" + id).equals(m.data().get("url"))));
    }

    /** docs/guide/firebase.md — the "Send a test notification" button of the web app and Postman. */
    @Test
    void testPushReportsTheChannelAndRemovesInvalidTokens() throws Exception {
        reset(pushSender);
        when(pushSender.mode()).thenReturn("firebase");
        Actor customer = newUser(Role.CUSTOMER);
        getAs(customer, "/api/v1/support/notifications/push-status")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("firebase"))
                .andExpect(jsonPath("$.devices").value(0));
        postJson(customer, "/api/v1/support/notifications/devices/test", "")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("NO_DEVICE_REGISTERED"));

        String good = "web-" + UUID.randomUUID();
        String expired = "web-" + UUID.randomUUID();
        for (String token : List.of(good, expired)) {
            postJson(customer, "/api/v1/support/notifications/devices",
                    "{\"token\":\"" + token + "\",\"platform\":\"WEB\"}").andExpect(status().isNoContent());
        }
        when(pushSender.send(any())).thenReturn(List.of(expired));
        postJson(customer, "/api/v1/support/notifications/devices/test", "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("firebase"))
                .andExpect(jsonPath("$.devices").value(2))
                .andExpect(jsonPath("$.invalidRemoved").value(1));
        verify(pushSender).send(argThat(m -> "TEST".equals(m.data().get("type")) && m.tokens().size() == 2));
        getAs(customer, "/api/v1/support/notifications/push-status").andExpect(jsonPath("$.devices").value(1));
    }

    @Test
    void testPushFailureIsReportedAsBadGateway() throws Exception {
        reset(pushSender);
        when(pushSender.send(any())).thenThrow(new PushSender.PushDeliveryException("FCM down", null));
        Actor agent = newUser(Role.SUPPORT_AGENT);
        postJson(agent, "/api/v1/support/notifications/devices",
                "{\"token\":\"web-" + UUID.randomUUID() + "\",\"platform\":\"WEB\"}").andExpect(status().isNoContent());
        postJson(agent, "/api/v1/support/notifications/devices/test", "")
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("PUSH_DELIVERY_FAILED"));
    }
}
