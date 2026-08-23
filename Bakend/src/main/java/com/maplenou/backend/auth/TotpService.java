package com.maplenou.backend.auth;

import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import org.springframework.stereotype.Service;

@Service
public class TotpService {

    public String generateSecret() {
        return new DefaultSecretGenerator(32).generate();
    }

    public String generateOtpAuthUri(String secret, String phoneNumber) {
        return "otpauth://totp/Maplenou:" + phoneNumber
                + "?secret=" + secret
                + "&issuer=Maplenou"
                + "&algorithm=SHA1"
                + "&digits=6"
                + "&period=30";
    }

    public boolean verifyCode(String secret, String code) {
        DefaultCodeVerifier verifier = new DefaultCodeVerifier(
                new DefaultCodeGenerator(),
                new SystemTimeProvider()
        );
        return verifier.isValidCode(secret, code);
    }
}
