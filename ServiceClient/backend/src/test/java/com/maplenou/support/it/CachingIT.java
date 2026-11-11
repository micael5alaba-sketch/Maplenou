package com.maplenou.support.it;

import com.maplenou.support.shared.Actor;
import com.maplenou.support.shared.Role;
import com.maplenou.support.ticket.application.CategoryService;
import com.maplenou.support.ticket.domain.Category;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Reference-data cache and HTTP revalidation (docs/guide/caching.md). */
class CachingIT extends IntegrationTest {

    @Autowired
    CategoryService categories;

    @Test
    void categoriesAreCachedAndRevalidatedWithEtag() throws Exception {
        Actor customer = newUser(Role.CUSTOMER);
        var first = getAs(customer, "/api/v1/support/categories")
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "max-age=300, private"))
                // V6: English description of the seeded categories
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$[?(@.code == 'DELIVERY')].descriptionEn").value(
                                org.hamcrest.Matchers.contains("Delivery tracking and incidents")))
                .andReturn();
        String etag = first.getResponse().getHeader("ETag");
        assertThat(etag).isNotBlank();
        // same content -> same validator
        assertThat(getAs(customer, "/api/v1/support/categories").andReturn().getResponse().getHeader("ETag"))
                .isEqualTo(etag);

        // same content -> 304 without a body
        mvc.perform(as(customer, get("/api/v1/support/categories")).header("If-None-Match", etag))
                .andExpect(status().isNotModified());
    }

    @Test
    void administratorChangeClearsTheCache() throws Exception {
        Actor customer = newUser(Role.CUSTOMER);
        Actor admin = newUser(Role.ADMINISTRATOR);
        String before = getAs(customer, "/api/v1/support/categories").andExpect(status().isOk())
                .andReturn().getResponse().getHeader("ETag");
        Category other = categories.find("PAYMENT");
        putJson(admin, "/api/v1/support/admin/categories/PAYMENT",
                "{\"name\":\"Paiement et facturation\",\"description\":\"" + other.description() + "\",\"active\":true}")
                .andExpect(status().isOk());
        // the snapshot is dropped after commit: the next read is fresh and gets a new validator
        var after = getAs(customer, "/api/v1/support/categories").andReturn().getResponse();
        assertThat(after.getContentAsString()).contains("Paiement et facturation");
        assertThat(after.getHeader("ETag")).isNotEqualTo(before);
        // restore for other tests sharing the database
        putJson(admin, "/api/v1/support/admin/categories/PAYMENT",
                "{\"name\":\"Paiement\",\"description\":\"" + other.description() + "\",\"active\":true}");
    }
}
