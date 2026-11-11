package com.maplenou.support.attachment.domain;

import com.maplenou.support.attachment.domain.AttachmentPolicy.IncomingFile;
import com.maplenou.support.shared.SupportException;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** DEC-003 / BR-040..BR-045. */
class AttachmentPolicyTest {

    static final long MB = 1024 * 1024;
    final AttachmentPolicy policy = new AttachmentPolicy(10 * MB, 5, 50 * MB);

    static byte[] png(int size) {
        byte[] b = new byte[size];
        byte[] sig = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        System.arraycopy(sig, 0, b, 0, sig.length);
        return b;
    }

    static byte[] pdf() {
        return "%PDF-1.7\n...".getBytes();
    }

    static byte[] jpeg() {
        return new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0};
    }

    static byte[] webp() {
        return "RIFF\0\0\0\0WEBPVP8 ".getBytes();
    }

    @Test
    void acceptsAllowedTypes() {
        var result = policy.validate(List.of(new IncomingFile("photo.png", png(100)), new IncomingFile("recu.pdf", pdf()),
                new IncomingFile("a.JPG", jpeg()), new IncomingFile("b.jpeg", jpeg()), new IncomingFile("c.webp", webp())), 0);
        assertThat(result).extracting(AttachmentPolicy.ValidatedFile::contentType)
                .containsExactly("image/png", "application/pdf", "image/jpeg", "image/jpeg", "image/webp");
    }

    @Test
    void rejectsDisallowedExtension() {
        assertThatThrownBy(() -> policy.validate(List.of(new IncomingFile("virus.exe", pdf())), 0))
                .isInstanceOf(SupportException.class).extracting("rule").isEqualTo("BR-040");
    }

    @Test
    void rejectsContentThatDoesNotMatchExtension() {
        assertThatThrownBy(() -> policy.validate(List.of(new IncomingFile("fake.png", "<html>".getBytes())), 0))
                .isInstanceOf(SupportException.class).extracting("code").isEqualTo("ATTACHMENT_CONTENT_MISMATCH");
    }

    @Test
    void rejectsFileOver10Mb() {
        assertThatThrownBy(() -> policy.validate(List.of(new IncomingFile("big.png", png((int) (10 * MB) + 1))), 0))
                .isInstanceOf(SupportException.class).extracting("rule").isEqualTo("BR-041");
    }

    @Test
    void acceptsExactly10Mb() {
        assertThat(policy.validate(List.of(new IncomingFile("ok.png", png((int) (10 * MB)))), 0)).hasSize(1);
    }

    @Test
    void rejectsMoreThan5FilesPerMessage() {
        List<IncomingFile> six = Collections.nCopies(6, new IncomingFile("p.pdf", pdf()));
        assertThatThrownBy(() -> policy.validate(six, 0))
                .isInstanceOf(SupportException.class).extracting("rule").isEqualTo("BR-042");
    }

    @Test
    void rejectsWhenTicketTotalWouldExceed50Mb() {
        assertThatThrownBy(() -> policy.validate(List.of(new IncomingFile("p.png", png((int) (2 * MB)))), 49 * MB))
                .isInstanceOf(SupportException.class).extracting("rule").isEqualTo("BR-043");
    }

    @Test
    void rejectsEmptyFile() {
        assertThatThrownBy(() -> policy.validate(List.of(new IncomingFile("e.pdf", new byte[0])), 0))
                .isInstanceOf(SupportException.class).extracting("code").isEqualTo("EMPTY_ATTACHMENT");
    }

    @Test
    void sanitizesFilenames() {
        assertThat(AttachmentPolicy.sanitizeFilename("../../etc/passwd.pdf")).isEqualTo("passwd.pdf");
        assertThat(AttachmentPolicy.sanitizeFilename("C:\\Users\\x\\reçu \"final\".pdf")).isEqualTo("reçu _final_.pdf");
        assertThat(AttachmentPolicy.sanitizeFilename(null)).isEqualTo("fichier");
        char[] longName = new char[300];
        Arrays.fill(longName, 'a');
        assertThat(AttachmentPolicy.sanitizeFilename(new String(longName) + ".pdf")).hasSize(200).endsWith(".pdf");
    }
}
