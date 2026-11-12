package com.maplenou.support.attachment.domain;

import com.maplenou.support.shared.SupportException;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Attachment rules from DEC-003 (BR-040..BR-045): JPG/JPEG/PNG/WEBP/PDF, 10 MB per file,
 * 5 files per message, 50 MB per ticket. The file type is detected from its content
 * (magic bytes) and must agree with the file extension; the client-declared type is ignored.
 */
public final class AttachmentPolicy {

    public record IncomingFile(String filename, byte[] content) {
    }

    public record ValidatedFile(String filename, String contentType, byte[] content) {
    }

    private static final Map<String, String> EXTENSIONS = Map.of(
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png",
            "webp", "image/webp",
            "pdf", "application/pdf");

    private final long maxFileBytes;
    private final int maxFilesPerMessage;
    private final long maxTicketBytes;

    public AttachmentPolicy(long maxFileBytes, int maxFilesPerMessage, long maxTicketBytes) {
        this.maxFileBytes = maxFileBytes;
        this.maxFilesPerMessage = maxFilesPerMessage;
        this.maxTicketBytes = maxTicketBytes;
    }

    /** Validates every file before anything is stored, so a rejection leaves the ticket untouched (BR-047). */
    public List<ValidatedFile> validate(List<IncomingFile> files, long existingTicketBytes) {
        if (files.size() > maxFilesPerMessage) {
            throw SupportException.businessRule("TOO_MANY_ATTACHMENTS",
                    "Maximum " + maxFilesPerMessage + " pièces jointes par message.", "BR-042");
        }
        long total = existingTicketBytes;
        List<ValidatedFile> result = new java.util.ArrayList<>();
        for (IncomingFile f : files) {
            String name = sanitizeFilename(f.filename());
            byte[] content = f.content();
            if (content == null || content.length == 0) {
                throw SupportException.businessRule("EMPTY_ATTACHMENT", "Le fichier « " + name + " » est vide.", "BR-045");
            }
            if (content.length > maxFileBytes) {
                throw SupportException.businessRule("ATTACHMENT_TOO_LARGE",
                        "Le fichier « " + name + " » dépasse 10 Mo.", "BR-041");
            }
            String byExtension = EXTENSIONS.get(extension(name));
            if (byExtension == null) {
                throw SupportException.businessRule("ATTACHMENT_TYPE_NOT_ALLOWED",
                        "Format non autorisé pour « " + name + " ». Formats acceptés : JPG, PNG, WEBP, PDF.", "BR-040");
            }
            String detected = detectType(content).orElse(null);
            if (!byExtension.equals(detected)) {
                throw SupportException.businessRule("ATTACHMENT_CONTENT_MISMATCH",
                        "Le contenu de « " + name + " » ne correspond pas à un fichier JPG, PNG, WEBP ou PDF valide.",
                        "BR-045");
            }
            total += content.length;
            if (total > maxTicketBytes) {
                throw SupportException.businessRule("TICKET_ATTACHMENTS_LIMIT",
                        "La taille totale des pièces jointes du ticket dépasserait 50 Mo.", "BR-043");
            }
            result.add(new ValidatedFile(name, detected, content));
        }
        return result;
    }

    /** Detects JPEG, PNG, WEBP and PDF from their signatures. */
    public static Optional<String> detectType(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return Optional.of("image/jpeg");
        }
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) {
            return Optional.of("image/png");
        }
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return Optional.of("image/webp");
        }
        if (b.length >= 5 && b[0] == '%' && b[1] == 'P' && b[2] == 'D' && b[3] == 'F' && b[4] == '-') {
            return Optional.of("application/pdf");
        }
        return Optional.empty();
    }

    static String extension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /** Keeps only the base name and strips control/path characters. */
    public static String sanitizeFilename(String raw) {
        String name = raw == null ? "fichier" : raw;
        name = name.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        name = name.replaceAll("[\\p{Cntrl}\"<>|:*?]", "_").trim();
        if (name.isEmpty()) {
            name = "fichier";
        }
        return name.length() > 200 ? name.substring(name.length() - 200) : name;
    }
}
