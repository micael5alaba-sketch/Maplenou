package com.maplenou.support.it;

import com.maplenou.support.demo.DemoUsers;
import com.maplenou.support.shared.Actor;
import com.maplenou.support.shared.Role;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** FR-011, BR-019, roadmap Oct. week 3 — AC-009. Uses the demo ecosystem adapter. */
class ContextIT extends IntegrationTest {

    private final Actor jean = DemoUsers.byId(DemoUsers.CUSTOMER_JEAN);
    private final Actor marie = DemoUsers.byId(DemoUsers.SELLER_MARIE);
    private final Actor kossi = DemoUsers.byId(DemoUsers.DRIVER_KOSSI);

    private static String withOrder(String category, String orderRef, String deliveryRef) {
        return """
                {"subject":"Ticket lié","description":"Détails","categoryCode":"%s",
                 "orderRef":%s,"deliveryRef":%s}
                """.formatted(category, orderRef == null ? "null" : "\"" + orderRef + "\"",
                deliveryRef == null ? "null" : "\"" + deliveryRef + "\"");
    }

    @Test
    void ac009_agentSeesOrderPaymentDeliveryAndSellerContext() throws Exception {
        existing(jean);
        Actor agent = newUser(Role.SUPPORT_AGENT);
        String id = createTicket(jean, withOrder("DELIVERY", "CMD-2026-0456", null));
        getAs(agent, "/api/v1/support/tickets/{id}/context", id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requester.name").value("Jean K."))
                .andExpect(jsonPath("$.order.data.ref").value("CMD-2026-0456"))
                .andExpect(jsonPath("$.order.data.amount").value(48500))
                .andExpect(jsonPath("$.payment.data.ref").value("PAY-2026-0456"))
                .andExpect(jsonPath("$.delivery.data.driverName").value("Kossi D."))
                .andExpect(jsonPath("$.seller.data.shopName").value("Boutique Marie K."))
                .andExpect(jsonPath("$.refund.linked").value(false));
    }

    @Test
    void requesterCannotSeeOperationalContext() throws Exception {
        existing(jean);
        String id = createTicket(jean, withOrder("ORDER", "CMD-2026-0456", null));
        getAs(jean, "/api/v1/support/tickets/{id}/context", id).andExpect(status().isForbidden());
        // requester detail shows their order but not payment/seller internals
        getAs(jean, "/api/v1/support/tickets/{id}", id)
                .andExpect(jsonPath("$.references.orderRef").value("CMD-2026-0456"))
                .andExpect(jsonPath("$.references.paymentRef").doesNotExist())
                .andExpect(jsonPath("$.references.sellerRef").doesNotExist());
    }

    @Test
    void br019_requesterCannotLinkSomeoneElsesOrder() throws Exception {
        Actor stranger = newUser(Role.CUSTOMER);
        postJson(stranger, "/api/v1/support/tickets", withOrder("ORDER", "CMD-2026-0456", null))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("REFERENCE_NOT_ACCESSIBLE"));
        postJson(stranger, "/api/v1/support/tickets", withOrder("ORDER", "CMD-DOES-NOT-EXIST", null))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("REFERENCE_NOT_ACCESSIBLE"));
    }

    @Test
    void sellerLinksAnOrderTheySold() throws Exception {
        existing(marie);
        Actor agent = newUser(Role.SUPPORT_AGENT);
        String id = createTicket(marie, withOrder("PAYMENT", "CMD-2026-0789", null));
        getAs(agent, "/api/v1/support/tickets/{id}/context", id)
                .andExpect(jsonPath("$.payment.data.commissionAmount").value(12400))
                .andExpect(jsonPath("$.payment.data.payoutStatus").value("NOT_PAID_OUT"));
    }

    @Test
    void driverLinksTheirDeliveryAndTheOrderIsDerived() throws Exception {
        existing(kossi);
        Actor agent = newUser(Role.SUPPORT_AGENT);
        String id = createTicket(kossi, withOrder("DELIVERY", null, "LIV-2026-0912"));
        getAs(agent, "/api/v1/support/tickets/{id}", id)
                .andExpect(jsonPath("$.references.deliveryRef").value("LIV-2026-0912"))
                .andExpect(jsonPath("$.references.orderRef").value("CMD-2026-0456"));
        // another driver cannot link it
        postJson(newUser(Role.DELIVERY_DRIVER), "/api/v1/support/tickets", withOrder("DELIVERY", null, "LIV-2026-0912"))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void myReferencesListsOwnOrdersOrDeliveries() throws Exception {
        getAs(existing(jean), "/api/v1/support/me/references")
                .andExpect(jsonPath("$.orders.length()").value(3))
                .andExpect(jsonPath("$.deliveries.length()").value(0));
        getAs(existing(kossi), "/api/v1/support/me/references")
                .andExpect(jsonPath("$.deliveries.length()").value(3));
        getAs(newUser(Role.CUSTOMER), "/api/v1/support/me/references")
                .andExpect(jsonPath("$.orders.length()").value(0));
    }
}
