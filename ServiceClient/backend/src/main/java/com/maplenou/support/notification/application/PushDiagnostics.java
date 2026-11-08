package com.maplenou.support.notification.application;

import com.maplenou.support.notification.application.port.DeviceTokenStore;
import com.maplenou.support.notification.application.port.MobilePushRelay;
import com.maplenou.support.notification.application.port.PushSender;
import com.maplenou.support.notification.domain.DeviceToken;
import com.maplenou.support.shared.Actor;
import com.maplenou.support.shared.SupportException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Lets a signed-in user check push delivery on their own devices: which channel is active (Firebase or log-only)
 * and a test message sent at once, outside the notification queue (no ticket, nothing stored).
 */
@Service
public class PushDiagnostics {

    static final String TEST_TITLE = "Notification de test";
    static final String TEST_BODY = "Les notifications push Maplenou fonctionnent sur cet appareil.";

    private final DeviceTokenStore deviceTokens;
    private final PushSender sender;
    private final MobilePushRelay relay;

    public PushDiagnostics(DeviceTokenStore deviceTokens, PushSender sender, MobilePushRelay relay) {
        this.deviceTokens = deviceTokens;
        this.sender = sender;
        this.relay = relay;
    }

    /** {@code mode}: "firebase" or "log-only"; {@code mobileRelay}: requesters also get pushes in the Maplenou app. */
    public record PushStatus(String mode, int devices, boolean mobileRelay) {
    }

    public record PushTestResult(String mode, int devices, int invalidRemoved) {
    }

    @Transactional(readOnly = true)
    public PushStatus status(Actor user) {
        return new PushStatus(sender.mode(), deviceTokens.forUser(user.id()).size(), relay.enabled());
    }

    @Transactional
    public PushTestResult sendTest(Actor user) {
        List<String> tokens = deviceTokens.forUser(user.id()).stream().map(DeviceToken::token).toList();
        if (tokens.isEmpty()) {
            throw SupportException.businessRule("NO_DEVICE_REGISTERED",
                    "Aucun appareil n'est enregistré pour les notifications push. Activez-les sur cet appareil "
                            + "puis réessayez.", null);
        }
        List<String> invalid;
        try {
            invalid = sender.send(new PushSender.PushMessage(tokens, TEST_TITLE, TEST_BODY, Map.of("type", "TEST")));
        } catch (PushSender.PushDeliveryException e) {
            throw SupportException.upstream("PUSH_DELIVERY_FAILED",
                    "Firebase n'a pas pu remettre la notification de test. Vérifiez la configuration Firebase.");
        }
        invalid.forEach(deviceTokens::deleteByToken);
        return new PushTestResult(sender.mode(), tokens.size(), invalid.size());
    }
}
