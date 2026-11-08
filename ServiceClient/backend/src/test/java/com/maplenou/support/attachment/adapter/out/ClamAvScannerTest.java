package com.maplenou.support.attachment.adapter.out;

import com.maplenou.support.attachment.adapter.out.scan.ClamAvScanner;
import com.maplenou.support.attachment.application.port.MalwareScanner.Verdict;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/** The clamd INSTREAM protocol against a tiny fake clamd (no ClamAV needed). */
class ClamAvScannerTest {

    /** Starts a one-shot fake clamd that answers with {@code reply.apply(receivedBytes)}. */
    private static int fakeClamd(Function<byte[], String> reply, AtomicReference<String> command) throws IOException {
        ServerSocket server = new ServerSocket(0);
        Thread t = new Thread(() -> {
            try (server; Socket s = server.accept()) {
                DataInputStream in = new DataInputStream(s.getInputStream());
                byte[] cmd = new byte[10];
                in.readFully(cmd);
                command.set(new String(cmd, StandardCharsets.US_ASCII));
                ByteArrayOutputStream data = new ByteArrayOutputStream();
                for (int len = in.readInt(); len > 0; len = in.readInt()) {
                    byte[] chunk = new byte[len];
                    in.readFully(chunk);
                    data.write(chunk);
                }
                s.getOutputStream().write((reply.apply(data.toByteArray()) + "\0").getBytes(StandardCharsets.US_ASCII));
                s.getOutputStream().flush();
            } catch (IOException ignored) {
                // the test fails on the scanner side
            }
        });
        t.setDaemon(true);
        t.start();
        return server.getLocalPort();
    }

    @Test
    void cleanFileIsAccepted() throws IOException {
        AtomicReference<String> cmd = new AtomicReference<>();
        byte[] file = new byte[200_000]; // several 64 KB chunks
        int port = fakeClamd(data -> data.length == file.length ? "stream: OK" : "stream: ERROR", cmd);
        var result = new ClamAvScanner("127.0.0.1", port, Duration.ofSeconds(5)).scan(file);
        assertThat(result.verdict()).isEqualTo(Verdict.CLEAN);
        assertThat(cmd.get()).isEqualTo("zINSTREAM\0");
    }

    @Test
    void infectedFileReportsTheSignature() throws IOException {
        int port = fakeClamd(data -> "stream: Eicar-Test-Signature FOUND", new AtomicReference<>());
        var result = new ClamAvScanner("127.0.0.1", port, Duration.ofSeconds(5))
                .scan("X5O!P%@AP[4\\PZX54(P^)7CC)7}$EICAR".getBytes(StandardCharsets.US_ASCII));
        assertThat(result.verdict()).isEqualTo(Verdict.INFECTED);
        assertThat(result.signature()).isEqualTo("Eicar-Test-Signature");
    }

    @Test
    void unreachableOrConfusedClamdIsUnavailable() throws IOException {
        int closedPort;
        try (ServerSocket s = new ServerSocket(0)) {
            closedPort = s.getLocalPort();
        }
        assertThat(new ClamAvScanner("127.0.0.1", closedPort, Duration.ofSeconds(1)).scan(new byte[10]).verdict())
                .isEqualTo(Verdict.UNAVAILABLE);
        int port = fakeClamd(data -> "INSTREAM size limit exceeded. ERROR", new AtomicReference<>());
        assertThat(new ClamAvScanner("127.0.0.1", port, Duration.ofSeconds(5)).scan(new byte[10]).verdict())
                .isEqualTo(Verdict.UNAVAILABLE);
    }
}
