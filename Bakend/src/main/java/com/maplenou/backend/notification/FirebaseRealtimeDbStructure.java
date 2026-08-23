package com.maplenou.backend.notification;

/**
 * Documentation de la structure Firebase Realtime Database utilisée par Maplenou.
 *
 * Le backend NE GÈRE PAS directement le GPS en temps réel — c'est le SDK Firebase
 * client (Flutter) qui écrit/lit les données directement, sans passer par le backend.
 *
 * Structure :
 * ─────────────────────────────────────────────────────────────────
 * /deliveries
 *   /{deliveryId}
 *     /location
 *       lat:      double   (latitude GPS du livreur)
 *       lng:      double   (longitude GPS du livreur)
 *       heading:  double   (cap en degrés, optionnel)
 *       updatedAt: long    (timestamp Unix ms)
 *
 * Règles d'accès Firebase (à configurer dans la console Firebase) :
 * ─────────────────────────────────────────────────────────────────
 * {
 *   "rules": {
 *     "deliveries": {
 *       "$deliveryId": {
 *         "location": {
 *           // Seul le livreur assigné peut écrire sa position
 *           ".write": "auth != null && auth.token.role === 'DELIVERY_AGENT'",
 *           // L'acheteur et le livreur peuvent lire
 *           ".read": "auth != null"
 *         }
 *       }
 *     }
 *   }
 * }
 *
 * Côté Flutter (livreur) :
 * ─────────────────────────────────────────────────────────────────
 *   FirebaseDatabase.instance
 *     .ref('deliveries/$deliveryId/location')
 *     .set({ 'lat': lat, 'lng': lng, 'updatedAt': DateTime.now().ms });
 *
 * Côté Flutter (acheteur — écoute temps réel) :
 * ─────────────────────────────────────────────────────────────────
 *   FirebaseDatabase.instance
 *     .ref('deliveries/$deliveryId/location')
 *     .onValue
 *     .listen((event) { ... });
 *
 * Le backend fournit les coordonnées de navigation via :
 *   GET /api/deliveries/{id}/navigation
 * (pickup = boutique, dropoff = adresse client)
 */
public final class FirebaseRealtimeDbStructure {
    private FirebaseRealtimeDbStructure() {}
}
