package com.maplenou.support.notification.adapter.out.push;

import com.maplenou.support.notification.application.port.PushSender;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;

import java.util.ArrayList;
import java.util.List;

/** Sends push notifications through Firebase Cloud Messaging. */
public class FirebasePushSender implements PushSender {

    private final FirebaseMessaging messaging;

    public FirebasePushSender(FirebaseMessaging messaging) {
        this.messaging = messaging;
    }

    @Override
    public List<String> send(PushMessage message) throws PushDeliveryException {
        MulticastMessage multicast = MulticastMessage.builder()
                .addAllTokens(message.tokens())
                .setNotification(com.google.firebase.messaging.Notification.builder()
                        .setTitle(message.title())
                        .setBody(message.body())
                        .build())
                .putAllData(message.data())
                // the web service worker (frontend/public/push-sw.js) reads title/body from the data as well
                .putData("title", message.title())
                .putData("body", message.body())
                .build();
        BatchResponse response;
        try {
            response = messaging.sendEachForMulticast(multicast);
        } catch (FirebaseMessagingException e) {
            throw new PushDeliveryException("FCM error: " + e.getMessagingErrorCode(), e);
        }
        List<String> invalid = new ArrayList<>();
        List<SendResponse> responses = response.getResponses();
        boolean anySuccess = false;
        boolean retryable = false;
        for (int i = 0; i < responses.size(); i++) {
            SendResponse r = responses.get(i);
            if (r.isSuccessful()) {
                anySuccess = true;
                continue;
            }
            MessagingErrorCode code = r.getException() == null ? null : r.getException().getMessagingErrorCode();
            if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT) {
                invalid.add(message.tokens().get(i));
            } else {
                retryable = true;
            }
        }
        if (!anySuccess && retryable) {
            throw new PushDeliveryException("FCM delivery failed for all devices", null);
        }
        return invalid;
    }
}
