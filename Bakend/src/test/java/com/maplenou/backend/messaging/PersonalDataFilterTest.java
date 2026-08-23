package com.maplenou.backend.messaging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersonalDataFilterTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "Appelle-moi au 90 12 34 56 stp",
            "Mon numero est 90112233",
            "Contact: contact@example.com",
            "Ecris-moi sur whatsapp",
            "On se voit sur Telegram",
            "Suis-moi @mon_pseudo123",
            "Ajoute-moi sur Instagram"
    })
    void detectsPersonalDataVariants(String message) {
        assertTrue(PersonalDataFilter.containsPersonalData(message),
                "devrait détecter des coordonnées personnelles dans : " + message);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Bonjour, le produit est-il disponible ?",
            "Merci beaucoup pour la livraison rapide",
            "Je voudrais 2 exemplaires en taille M",
            ""
    })
    void allowsOrdinaryMessages(String message) {
        assertFalse(PersonalDataFilter.containsPersonalData(message),
                "ne devrait pas bloquer un message ordinaire : " + message);
    }

    @Test
    void nullIsNotFlagged() {
        assertFalse(PersonalDataFilter.containsPersonalData(null));
    }
}
