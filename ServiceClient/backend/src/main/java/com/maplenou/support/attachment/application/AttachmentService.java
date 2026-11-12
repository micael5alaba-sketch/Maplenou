package com.maplenou.support.attachment.application;

import com.maplenou.support.attachment.application.port.BinaryStorage;
import com.maplenou.support.attachment.application.port.MalwareScanner;
import com.maplenou.support.attachment.domain.AttachmentPolicy;
import com.maplenou.support.attachment.domain.AttachmentPolicy.IncomingFile;
import com.maplenou.support.attachment.domain.AttachmentPolicy.ValidatedFile;
import com.maplenou.support.shared.SupportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * Public API of the attachment module: validates files by content (DEC-003), scans them with the antivirus when
 * one is configured, and stores them. A file written during a transaction that rolls back is removed (BR-047).
 */
@Service
public class AttachmentService {

    private static final Logger log = LoggerFactory.getLogger(AttachmentService.class);

    /** A stored file and its fingerprint. */
    public record StoredFile(String key, String filename, String contentType, long size, String sha256) {
    }

    private final AttachmentPolicy policy;
    private final BinaryStorage storage;
    private final MalwareScanner scanner;
    private final boolean failClosed;

    public AttachmentService(AttachmentLimits limits, BinaryStorage storage, MalwareScanner scanner) {
        this.policy = new AttachmentPolicy(limits.maxFileBytes(), limits.maxFilesPerMessage(), limits.maxTicketBytes());
        this.storage = storage;
        this.scanner = scanner;
        this.failClosed = limits.failClosed();
    }

    /** Checks every file before anything is stored, so a rejection leaves the ticket untouched. */
    public List<ValidatedFile> validate(List<IncomingFile> files, long existingTicketBytes) {
        List<ValidatedFile> valid = policy.validate(files, existingTicketBytes);
        for (ValidatedFile f : valid) {
            MalwareScanner.ScanResult result = scanner.scan(f.content());
            switch (result.verdict()) {
                case INFECTED -> {
                    log.warn("Attachment refused by the antivirus ({}): {}", result.signature(), f.filename());
                    throw SupportException.businessRule("ATTACHMENT_INFECTED",
                            "Le fichier « " + f.filename() + " » a été refusé par l'antivirus.", "BR-045");
                }
                case UNAVAILABLE -> {
                    if (failClosed) {
                        throw SupportException.unavailable("ANTIVIRUS_UNAVAILABLE",
                                "Les pièces jointes ne peuvent pas être vérifiées pour le moment. Réessayez plus tard.");
                    }
                    log.warn("Antivirus unavailable, file accepted without scan: {}", f.filename());
                }
                case CLEAN -> { }
            }
        }
        return valid;
    }

    /** Stores a validated file under {@code key}; removed again if the current transaction rolls back. */
    public StoredFile store(String key, ValidatedFile file) {
        storage.store(key, file.content(), file.contentType());
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) {
                        try {
                            storage.delete(key);
                        } catch (RuntimeException ignored) {
                            // best effort cleanup
                        }
                    }
                }
            });
        }
        return new StoredFile(key, file.filename(), file.contentType(), file.content().length, sha256(file.content()));
    }

    public Optional<BinaryStorage.Content> load(String key) {
        return storage.load(key);
    }

    public record Diagnostics(String storage, String antivirus) {
    }

    public Diagnostics diagnostics() {
        return new Diagnostics(storage.name(), scanner.name());
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
