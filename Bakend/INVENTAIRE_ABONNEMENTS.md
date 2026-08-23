# Inventaire des abonnements — Backend Maplenou

Liste de tous les comptes/abonnements externes dont le backend a besoin pour tourner en
production. Les tarifs indiqués sont des **ordres de grandeur à vérifier au moment de souscrire**
(ils évoluent régulièrement) — pas des prix contractuels.

---

## 1. Critique — bloquant pour tout lancement

| Service | Pourquoi le backend en a besoin | Coût indicatif | Priorité |
|---|---|---|---|
| **Hébergement serveur**<br>(Railway, Render, Fly.io, DigitalOcean…) | Fait tourner l'application Spring Boot en continu, accessible publiquement | ~5 – 25 $/mois | 🔴 Critique |
| **Redis managé**<br>(Upstash, Redis Cloud, AWS ElastiCache) | Authentification entière en dépend (connexion, 2FA, anti-abus) — actuellement remplacé par une instance de test qui disparaît à chaque redémarrage | Gratuit au départ, puis ~10 $/mois | 🔴 Critique |
| **PostgreSQL managé**<br>(souvent inclus à l'hébergeur, sinon Neon, Supabase) | Base de données principale — commandes, comptes, catalogue | Gratuit au départ, puis ~15 $/mois | 🔴 Critique |
| **Cloudinary** | Stockage des photos produits et des preuves de livraison | Offre gratuite généreuse au démarrage | 🔴 Critique |
| **Nom de domaine** | Adresse publique de l'API et du lien admin sécurisé | ~10 – 15 $/an | 🔴 Critique |

**Estimation plancher pour démarrer : ~15 – 40 $/mois**

---

## 2. Important — à vérifier / activer bientôt

| Service | Pourquoi le backend en a besoin | Coût indicatif | Priorité |
|---|---|---|---|
| **Firebase**<br>(Cloud Messaging + Realtime Database) | Notifications push (commande, livraison) **et** position GPS en direct du livreur pendant sa course — l'app Flutter écrit/lit les coordonnées directement dans Firebase, sans passer par le backend. Un projet existe déjà dans le code : confirmer que c'est bien celui de production, et configurer les règles de sécurité (seul le livreur assigné peut écrire sa position) | Gratuit jusqu'à un volume raisonnable | 🟡 À vérifier |
| **Certificat HTTPS** | Chiffrement obligatoire pour toute API en production | Généralement gratuit et automatique via l'hébergeur (Let's Encrypt) | 🟡 À confirmer |

---

## 3. Cartographie — pas d'abonnement nécessaire pour l'instant

**Aucun compte Google Maps Platform n'est requis actuellement.**

Le backend stocke de simples coordonnées GPS (boutiques, adresses clients) et les transmet au
livreur via `GET /api/deliveries/{id}/navigation` ; c'est l'app Flutter qui ouvre ensuite l'app
Google Maps installée sur le téléphone avec un lien direct — ça ne demande aucune clé API ni
frais. Il n'y a pas non plus de géocodage (convertir une adresse texte en coordonnées), pas
d'autocomplétion d'adresse, et les zones de livraison ont un tarif et un délai saisis à la main
par l'admin plutôt que calculés depuis une distance réelle.

> Si vous voulez plus tard l'autocomplétion d'adresse, la conversion automatique adresse→GPS, ou
> un calcul réel de distance/temps par zone, il faudra alors un compte **Google Maps Platform**
> (Geocoding + Places + Distance Matrix — facturation à l'usage, avec un crédit gratuit mensuel)
> ou une alternative comme Mapbox. **Pas urgent pour lancer.**

---

## 4. Paiement — décision business à prendre avant de souscrire

| Service | Pourquoi le backend en a besoin | Coût indicatif | Priorité |
|---|---|---|---|
| **FedaPay** | Seul moyen d'encaisser en mobile money (T-Money, Flooz) — non techniquement intégré au backend pour l'instant, reporté par choix | Pas d'abonnement fixe, frais par transaction | 🔴 Bloquant business |
| **Stripe** | Paiement carte bancaire | Frais par transaction | 🔴 Bloqué |

> **Point bloquant connu :** un compte marchand Stripe n'est actuellement pas ouvrable pour une
> société basée au Togo. Deux pistes à trancher avec le porteur du projet : ouvrir l'entité légale
> ailleurs, ou vérifier si FedaPay peut aussi couvrir le paiement par carte. **Ne rien souscrire
> côté carte bancaire avant cette décision.**

---

## 5. À clarifier avant de signer quoi que ce soit

> **Transporteur tiers (transport principal) :** le cahier des charges cite Colissimo à titre
> d'exemple — c'est un service de La Poste française, probablement inadapté à une livraison
> Togo/Bénin. Le backend a une structure technique prête à brancher n'importe quel transporteur,
> mais **identifier le vrai partenaire logistique local** avant de signer un contrat ou de payer
> un abonnement au nom de "Colissimo".

---

## 6. Optionnel — pas bloquant pour lancer

| Service | Pourquoi le backend en a besoin | Coût indicatif | Priorité |
|---|---|---|---|
| **Outil d'emailing**<br>(Mailchimp, Brevo, SendGrid) | Le backend capture déjà les emails inscrits — l'envoi réel de newsletters nécessitera cet outil plus tard | Offres gratuites disponibles | ⚪ Plus tard |
| **Supervision**<br>(UptimeRobot, Sentry) | Être alerté si le serveur tombe ou qu'une erreur se répète — pas dans le cahier des charges mais recommandé avant l'ouverture au public | Offres gratuites de base | ⚪ Recommandé |

---

*Document préparé le 23 août 2026 — tarifs indicatifs, à vérifier à la souscription.*
