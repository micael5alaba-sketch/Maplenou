package com.maplenou.backend.auth;

import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TotpServiceTest {

    private final TotpService totpService = new TotpService();

    @Test
    void generatesA32CharacterSecret() {
        String secret = totpService.generateSecret();
        assertNotNull(secret);
        assertTrue(secret.length() >= 16, "le secret TOTP doit être suffisamment long");
    }

    @Test
    void otpAuthUriContainsSecretAndIssuer() {
        String secret = totpService.generateSecret();
        String uri = totpService.generateOtpAuthUri(secret, "+22890000000");
        assertTrue(uri.startsWith("otpauth://totp/"));
        assertTrue(uri.contains("secret=" + secret));
        assertTrue(uri.contains("issuer=Maplenou"));
    }

    @Test
    void verifiesAValidCurrentCode() throws Exception {
        String secret = totpService.generateSecret();
        String currentCode = new DefaultCodeGenerator().generate(secret,
                new SystemTimeProvider().getTime() / 30);

        assertTrue(totpService.verifyCode(secret, currentCode));
    }

    @Test
    void rejectsAnObviouslyWrongCode() {
        String secret = totpService.generateSecret();
        assertFalse(totpService.verifyCode(secret, "000000"));
    }
}
