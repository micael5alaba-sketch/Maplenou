package com.maplenou.support.attachment.adapter.out.scan;

import com.maplenou.support.attachment.application.port.MalwareScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Scans files with ClamAV ({@code clamd} INSTREAM command over TCP, compose profile {@code antivirus}). The file is
 * sent in chunks; clamd answers {@code stream: OK} or {@code stream: <signature> FOUND}.
 */
public class ClamAvScanner implements MalwareScanner {

    private static final Logger log = LoggerFactory.getLogger(ClamAvScanner.class);
    private static final int CHUNK = 64 * 1024;

    private final String host;
    private final int port;
    private final int timeoutMillis;

    public ClamAvScanner(String host, int port, Duration timeout) {
        this.host = host;
        this.port = port;
        this.timeoutMillis = (int) timeout.toMillis();
    }

    @Override
    public ScanResult scan(byte[] content) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMillis);
            socket.setSoTimeout(timeoutMillis);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.write("zINSTREAM\0".getBytes(StandardCharsets.US_ASCII));
            for (int offset = 0; offset < content.length; offset += CHUNK) {
                int len = Math.min(CHUNK, content.length - offset);
                out.writeInt(len);
                out.write(content, offset, len);
            }
            out.writeInt(0);
            out.flush();
            String reply = readReply(socket.getInputStream()).trim();
            if (reply.endsWith("OK")) {
                return ScanResult.CLEAN;
            }
            if (reply.endsWith("FOUND")) {
                String signature = reply.replaceFirst("^stream:\\s*", "").replaceFirst("\\s*FOUND$", "");
                return new ScanResult(Verdict.INFECTED, signature);
            }
            log.warn("Unexpected clamd reply: {}", reply);
            return new ScanResult(Verdict.UNAVAILABLE, null);
        } catch (IOException e) {
            log.warn("clamd unreachable at {}:{}: {}", host, port, e.getMessage());
            return new ScanResult(Verdict.UNAVAILABLE, null);
        }
    }

    private static String readReply(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int b;
        while ((b = in.read()) != -1 && b != 0) {
            buffer.write(b);
            if (buffer.size() > 4096) {
                break;
            }
        }
        return buffer.toString(StandardCharsets.US_ASCII);
    }

    @Override
    public String name() {
        return "clamav";
    }
}
