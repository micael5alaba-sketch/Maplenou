package com.maplenou.support.it;

import com.maplenou.support.shared.Actor;
import com.maplenou.support.shared.Role;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** DEC-CS10-16: default SLA mode is 24/7 (maplenou.sla.mode=always). */
class SlaModeIT extends IntegrationTest {

    @Test
    void deadlinesAreWallClockTimeAndTheModeIsVisibleToManagers() throws Exception {
        Actor customer = newUser(Role.CUSTOMER);
        Actor agent = newUser(Role.SUPPORT_AGENT);
        Actor manager = newUser(Role.SUPPORT_MANAGER);
        String id = createTicket(customer, ticketJson("DELIVERY", "HIGH"));
        var ticket = getAs(agent, "/api/v1/support/tickets/{id}", id);
        Instant created = Instant.parse(json(ticket, "$.createdAt"));
        Instant firstDue = Instant.parse(json(ticket, "$.sla.firstResponse.dueAt"));
        Instant resolutionDue = Instant.parse(json(ticket, "$.sla.resolution.dueAt"));
        // whatever the day and hour of the test run: 4 h and 24 h exactly
        assertThat(Duration.between(created, firstDue).toMinutes()).isBetween(239L, 240L);
        assertThat(Duration.between(created, resolutionDue).toHours()).isBetween(23L, 24L);

        getAs(manager, "/api/v1/support/admin/sla-settings")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("always"))
                .andExpect(jsonPath("$.minutesPerDay").value(1440));
        getAs(customer, "/api/v1/support/admin/sla-settings").andExpect(status().isForbidden());
    }
}
