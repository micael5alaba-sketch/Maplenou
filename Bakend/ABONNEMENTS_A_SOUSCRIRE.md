# Abonnements à souscrire pour la mise en ligne de Maplenou

Liste finale, un par un, pour mettre en ligne le **front-end** (app Flutter, version web) et le
**back-end** (serveur Spring Boot + base de données + Redis) chez le même hébergeur,
**Render** — recommandé parce qu'il regroupe tout ça au même endroit, avec HTTPS automatique et
gratuit, et des sauvegardes automatiques sur la base de données.

**Facturation annuelle choisie partout où c'est possible** (moins chère qu'au mois, ~15-20% de
remise chez Render — pourcentage exact confirmé par Render au moment de payer, pas avant).

Nom de domaine volontairement **retiré de cette liste** — Render fournit une adresse gratuite
(`xxx.onrender.com`) suffisante pour démarrer ; à ajouter plus tard séparément si besoin.

---

## 1. Render — Serveur back-end (Web Service)

- **Pourquoi** : fait tourner le serveur Spring Boot en continu, accessible depuis Internet.
- **Formule** : Standard (2 Go de mémoire — nécessaire pour Spring Boot, la formule la moins chère
  à 7 $/mois manque de mémoire).
- **Coût annuel** : **~246 $/an** (≈ 148 000 FCFA/an)
- **Action** : créer un compte Render, choisir "Web Service", formule Standard, facturation annuelle.

## 2. Render — Base de données PostgreSQL

- **Pourquoi** : stocke toutes les données (comptes, commandes, catalogue, livraisons…).
- **Formule** : Starter (suffisant pour démarrer).
- **Coût annuel** : **~69 $/an** (≈ 41 000 FCFA/an)
- **Action** : dans le même compte Render, ajouter une base "PostgreSQL", formule Starter,
  facturation annuelle.

## 3. Render — Redis

- **Pourquoi** : indispensable au fonctionnement de la connexion (pas une option) — gère la
  déconnexion, le code à double facteur, et bloque les tentatives de piratage par force brute.
- **Formule** : Starter.
- **Coût annuel** : **~98 $/an** (≈ 59 000 FCFA/an)
- **Action** : dans le même compte Render, ajouter "Redis" (Key Value), formule Starter,
  facturation annuelle.

## 4. Render — Site statique (front-end, version web de l'app)

- **Pourquoi** : héberge la version web de l'app Flutter.
- **Formule** : gratuite (jusqu'à 100 Go de visites par mois — largement suffisant au départ).
- **Coût annuel** : **0 $**
- **Action** : dans le même compte Render, ajouter "Static Site", connecter le dossier de build
  Flutter web.

## 5. Compte développeur Google Play

- **Pourquoi** : obligatoire pour publier l'app sur le Play Store.
- **Coût** : **25 $, une seule fois** (≈ 15 000 FCFA) — pas un abonnement annuel, payé une fois
  pour toujours.
- **Action** : créer un compte sur [play.google.com/console](https://play.google.com/console),
  vérification d'identité à prévoir (quelques jours).

## 6. Cloudinary (photos produits, preuves de livraison)

- **Pourquoi** : déjà branché dans le code.
- **Coût annuel** : **0 $** — offre gratuite suffisante au démarrage.
- **Action** : créer un compte sur [cloudinary.com](https://cloudinary.com), récupérer les clés,
  les donner pour configuration (jamais en clair dans le code).

## 7. Firebase (notifications + position GPS en direct du livreur)

- **Pourquoi** : déjà branché dans le code.
- **Coût annuel** : **0 $** — gratuit jusqu'à un volume d'utilisateurs raisonnable.
- **Action** : déjà fait — à juste confirmer que c'est le bon projet (production, pas un projet
  de test).

---

## Total

| | Coût annuel |
|---|---|
| Render (back-end + front-end, les 4 lignes ci-dessus) | **~413 $/an** (≈ 248 000 FCFA/an) |
| Compte Google Play (une fois) | 25 $ (≈ 15 000 FCFA) |
| Cloudinary, Firebase | 0 $ |
| **Total première année** | **~438 $** (≈ 263 000 FCFA) |
| **Total années suivantes** | **~413 $/an** (≈ 248 000 FCFA/an) |

**Non inclus dans ce total** (pas des abonnements fixes) :
- **FedaPay** : pas d'abonnement, une commission de ~1,8 % à 3,6 % prélevée sur chaque vente réelle.
- **Nom de domaine** : volontairement retiré de cette liste, à ajouter plus tard (~12 $/an) si
  souhaité.

---

*Tarifs indicatifs 2026, à confirmer au moment de payer chez Render (le pourcentage exact de la
remise annuelle s'affiche sur l'écran de paiement) — document préparé pour servir de check-list,
un abonnement à la fois.*
