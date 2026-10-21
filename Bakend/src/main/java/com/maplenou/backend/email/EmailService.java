package com.maplenou.backend.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Façade d'envoi d'email — désactivée par défaut ({@code app.brevo.enabled}, comme
 * {@code app.fedapay.enabled}/{@code app.colissimo.enabled}) : sans clé Brevo réelle (dev local,
 * CI), le code est simplement loggué au lieu d'échouer, pour ne pas bloquer le développement.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final BrevoEmailClient brevoEmailClient;

    @Value("${app.brevo.enabled}")
    private boolean enabled;

    public void sendPasswordResetCode(String toEmail, String code) {
        String subject = "Votre code de réinitialisation Maplenou";
        String html = """
                <p>Bonjour,</p>
                <p>Voici votre code pour réinitialiser votre mot de passe Maplenou :</p>
                <p style="font-size:24px;font-weight:bold;letter-spacing:4px;">%s</p>
                <p>Ce code expire dans 10 minutes. Si vous n'êtes pas à l'origine de cette demande, ignorez cet email.</p>
                """.formatted(code);

        if (!enabled) {
            log.warn("Brevo désactivé (app.brevo.enabled=false) — code de réinitialisation pour {} : {}", toEmail, code);
            return;
        }
        brevoEmailClient.sendEmail(toEmail, subject, html);
    }
}
