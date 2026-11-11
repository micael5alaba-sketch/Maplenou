package com.maplenou.support.attachment.adapter.out.storage;

import com.maplenou.support.attachment.application.port.BinaryStorage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

/** Stores attachments under a private directory; keys are server-generated, never user input. */
public class LocalFileStorage implements BinaryStorage {

    private final Path root;

    public LocalFileStorage(String directory) {
        this.root = Path.of(directory).toAbsolutePath().normalize();
    }

    @Override
    public void store(String key, byte[] content, String contentType) {
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not store attachment", e);
        }
    }

    @Override
    public Optional<Content> load(String key) {
        Path p = resolve(key);
        return Files.isReadable(p) ? Optional.of(() -> Files.newInputStream(p)) : Optional.empty();
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not delete attachment", e);
        }
    }

    @Override
    public String name() {
        return "local";
    }

    private Path resolve(String key) {
        Path p = root.resolve(key).normalize();
        if (!p.startsWith(root)) {
            throw new IllegalArgumentException("Invalid storage key");
        }
        return p;
    }
}
