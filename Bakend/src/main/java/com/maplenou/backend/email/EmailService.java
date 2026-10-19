package com.maplenou.backend.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Façade d'envoi d'email — désactivée par défaut ({@code app.mailjet.enabled}, comme
 * {@code app.fedapay.enabled}/{@code app.colissimo.enabled}) : sans clé Mailjet réelle (dev local,
 * CI), le code est simplement loggué au lieu d'échouer, pour ne pas bloquer le développement.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final MailjetEmailClient mailjetEmailClient;

    @Value("${app.mailjet.enabled}")
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
            log.warn("Mailjet désactivé (app.mailjet.enabled=false) — code de réinitialisation pour {} : {}", toEmail, code);
            return;
        }
        mailjetEmailClient.sendEmail(toEmail, subject, html);
    }
}
