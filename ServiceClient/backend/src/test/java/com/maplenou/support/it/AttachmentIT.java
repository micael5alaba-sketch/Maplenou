package com.maplenou.support.it;

import com.maplenou.support.shared.Actor;
import com.maplenou.support.shared.Role;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** FR-010, DEC-003 — AC-017, AC-018, BR-040..BR-047. */
class AttachmentIT extends IntegrationTest {

    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};

    private MockMultipartHttpServletRequestBuilder message(String ticketId, String body, MockMultipartFile... files) {
        var req = multipart("/api/v1/support/tickets/{id}/messages", ticketId)
                .file(new MockMultipartFile("message", "", MediaType.APPLICATION_JSON_VALUE,
                        ("{\"body\":\"" + body + "\"}").getBytes()));
        for (MockMultipartFile f : files) {
            req.file(f);
        }
        return req;
    }

    @Test
    void ac018_validAttachmentIsAcceptedAndOnlyAuthorizedUsersCanDownload() throws Exception {
        Actor customer = newUser(Role.CUSTOMER);
        Actor other = newUser(Role.CUSTOMER);
        Actor agent = newUser(Role.SUPPORT_AGENT);
        String id = createTicket(customer);
        var r = mvc.perform(as(customer, message(id, "Photo du colis",
                        new MockMultipartFile("files", "colis.png", "image/png", PNG))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.attachments[0].filename").value("colis.png"))
                .andExpect(jsonPath("$.attachments[0].contentType").value("image/png"))
                .andExpect(jsonPath("$.attachments[0].sizeBytes").value(PNG.length));
        String attachmentId = json(r, "$.attachments[0].id");

        getAs(customer, "/api/v1/support/tickets/{id}/attachments/{a}", id, attachmentId)
                .andExpect(status().isOk())
                .andExpect(content().bytes(PNG))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.startsWith("attachment")));
        getAs(agent, "/api/v1/support/tickets/{id}/attachments/{a}", id, attachmentId).andExpect(status().isOk());
        getAs(other, "/api/v1/support/tickets/{id}/attachments/{a}", id, attachmentId).andExpect(status().isNotFound());
    }

    @Test
    void ticketCanBeCreatedWithAttachments() throws Exception {
        Actor customer = newUser(Role.CUSTOMER);
        var r = mvc.perform(as(customer, multipart("/api/v1/support/tickets")
                        .file(new MockMultipartFile("ticket", "", MediaType.APPLICATION_JSON_VALUE,
                                ticketJson("ORDER", null).getBytes()))
                        .file(new MockMultipartFile("files", "recu.pdf", "application/pdf", "%PDF-1.4 x".getBytes()))))
                .andExpect(status().isCreated());
        String id = json(r, "$.id");
        List<String> names = json(getAs(customer, "/api/v1/support/tickets/{id}/messages", id), "$[0].attachments[*].filename");
        assertThat(names).containsExactly("recu.pdf");
    }

    @Test
    void ac017_unsupportedTypeIsRejectedWithoutSideEffects() throws Exception {
        Actor customer = newUser(Role.CUSTOMER);
        String id = createTicket(customer);
        mvc.perform(as(customer, message(id, "Fichier",
                        new MockMultipartFile("files", "script.exe", "application/octet-stream", "MZ...".getBytes()))))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.rule").value("BR-040"));
        mvc.perform(as(customer, message(id, "Faux PNG",
                        new MockMultipartFile("files", "faux.png", "image/png", "<svg onload=alert(1)>".getBytes()))))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("ATTACHMENT_CONTENT_MISMATCH"));
        // the rejected messages were not stored (BR-047)
        List<String> bodies = json(getAs(customer, "/api/v1/support/tickets/{id}/messages", id), "$[*].body");
        assertThat(bodies).doesNotContain("Fichier", "Faux PNG");
    }

    @Test
    void ac017_moreThanFiveFilesIsRejected() throws Exception {
        Actor customer = newUser(Role.CUSTOMER);
        String id = createTicket(customer);
        MockMultipartFile[] six = new MockMultipartFile[6];
        for (int i = 0; i < 6; i++) {
            six[i] = new MockMultipartFile("files", "p" + i + ".png", "image/png", PNG);
        }
        mvc.perform(as(customer, message(id, "Trop", six)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.rule").value("BR-042"));
    }

    @Test
    void ac017_fileOver10MbIsRejected() throws Exception {
        Actor customer = newUser(Role.CUSTOMER);
        String id = createTicket(customer);
        byte[] big = new byte[10 * 1024 * 1024 + 1];
        System.arraycopy(PNG, 0, big, 0, PNG.length);
        mvc.perform(as(customer, message(id, "Gros", new MockMultipartFile("files", "gros.png", "image/png", big))))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.rule").value("BR-041"));
    }
}
