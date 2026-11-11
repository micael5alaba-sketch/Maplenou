package com.maplenou.support.it;

import com.maplenou.support.shared.Actor;
import com.maplenou.support.shared.Role;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** FR-014 — search and filters (docs/architecture/api-specification.md §Search). */
class SearchIT extends IntegrationTest {

    @Test
    void filtersCombineAndAreScopedPerRequester() throws Exception {
        Actor seller = newUser(Role.SELLER);
        Actor agent = newUser(Role.SUPPORT_AGENT);
        String payment = createTicket(seller, ticketJson("PAYMENT", "HIGH"));
        String order = createTicket(seller, ticketJson("ORDER", "LOW"));
        String other = createTicket(seller, ticketJson("OTHER", "MEDIUM"));
        assignSelf(agent, payment);

        String base = "/api/v1/support/tickets?requesterId=" + seller.id();
        assertThat(ids(agent, base)).containsExactlyInAnyOrder(payment, order, other);
        assertThat(ids(agent, base + "&category=PAYMENT")).containsExactly(payment);
        assertThat(ids(agent, base + "&priority=LOW&priority=MEDIUM")).containsExactlyInAnyOrder(order, other);
        assertThat(ids(agent, base + "&status=ASSIGNED")).containsExactly(payment);
        assertThat(ids(agent, base + "&assignee=unassigned")).containsExactlyInAnyOrder(order, other);
        assertThat(ids(agent, base + "&assignee=me")).containsExactly(payment);
        assertThat(ids(agent, base + "&assignee=" + agent.id())).containsExactly(payment);
        assertThat(ids(agent, base + "&requesterRole=SELLER")).hasSize(3);
        assertThat(ids(agent, base + "&requesterRole=CUSTOMER")).isEmpty();
        assertThat(ids(agent, base + "&open=true")).hasSize(3);
        assertThat(ids(agent, base + "&slaBreached=true")).isEmpty();
        assertThat(ids(agent, base + "&createdFrom=2000-01-01&createdTo=2000-01-02")).isEmpty();
    }

    @Test
    void searchByTicketNumberOrSubject() throws Exception {
        Actor customer = newUser(Role.CUSTOMER);
        Actor agent = newUser(Role.SUPPORT_AGENT);
        String marker = "zq" + UUID.randomUUID().toString().substring(0, 8);
        String id = createTicket(customer, """
                {"subject":"Colis jamais reçu %s","description":"d","categoryCode":"DELIVERY"}""".formatted(marker));
        String number = json(getAs(customer, "/api/v1/support/tickets/{id}", id), "$.ticketNumber");
        assertThat(ids(agent, "/api/v1/support/tickets?q=" + number)).containsExactly(id);
        assertThat(ids(agent, "/api/v1/support/tickets?q=" + marker.toUpperCase())).containsExactly(id);
        assertThat(ids(customer, "/api/v1/support/tickets?q=" + marker)).containsExactly(id);
        assertThat(ids(customer, "/api/v1/support/tickets?q=" + number.toLowerCase())).containsExactly(id);
    }

    @Test
    void paginationAndSorting() throws Exception {
        Actor customer = newUser(Role.CUSTOMER);
        for (String p : new String[]{"LOW", "HIGH", "MEDIUM"}) {
            createTicket(customer, ticketJson("ORDER", p));
        }
        getAs(customer, "/api/v1/support/tickets?size=2&page=0&sort=priorityRank,desc")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.items[0].priority").value("HIGH"))
                .andExpect(jsonPath("$.items[1].priority").value("MEDIUM"));
        getAs(customer, "/api/v1/support/tickets?sort=password,asc")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_SORT"));
        getAs(customer, "/api/v1/support/tickets?assignee=nobody")
                .andExpect(status().isBadRequest());
        getAs(customer, "/api/v1/support/tickets?status=UNKNOWN")
                .andExpect(status().isBadRequest());
    }

    @Test
    void supportListShowsSlaStateRequesterListDoesNot() throws Exception {
        Actor customer = newUser(Role.CUSTOMER);
        Actor agent = newUser(Role.SUPPORT_AGENT);
        createTicket(customer);
        getAs(agent, "/api/v1/support/tickets?requesterId=" + customer.id())
                .andExpect(jsonPath("$.items[0].slaState").isNotEmpty());
        getAs(customer, "/api/v1/support/tickets").andExpect(jsonPath("$.items[0].slaState").doesNotExist());
    }

    private List<String> ids(Actor user, String url) throws Exception {
        return json(getAs(user, url).andExpect(status().isOk()), "$.items[*].id");
    }
}
