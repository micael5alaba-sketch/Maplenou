package com.maplenou.backend.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, UUID> {

    /** Tous les tokens FCM d'un utilisateur (pour envoyer sur tous ses appareils). */
    List<DeviceToken> findByUserId(UUID userId);

    /** Recherche par valeur de token (pour éviter les doublons à l'enregistrement). */
    Optional<DeviceToken> findByToken(String token);

    /** Supprime un token précis (déconnexion d'un appareil). */
    @Modifying
    @Query("DELETE FROM DeviceToken dt WHERE dt.token = :token AND dt.user.id = :userId")
    void deleteByTokenAndUserId(@Param("token") String token, @Param("userId") UUID userId);

    /** Supprime tous les tokens d'un utilisateur (déconnexion globale). */
    @Modifying
    @Query("DELETE FROM DeviceToken dt WHERE dt.user.id = :userId")
    void deleteAllByUserId(@Param("userId") UUID userId);
}
