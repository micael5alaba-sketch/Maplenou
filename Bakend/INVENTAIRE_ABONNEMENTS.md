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
| **VPN d'entreprise ou IP fixe**<br>(ex. Tailscale, NordLayer, ou option "IP fixe" chez le fournisseur internet du bureau) | La restriction d'accès au back-office Admin (`/api/admin/**`) est maintenant codée (liste blanche IP/CIDR, `ADMIN_ALLOWED_IPS`), mais elle est **désactivée tant qu'aucune IP n'est fournie**. Il faut décider par où l'équipe Admin se connectera : IP fixe du bureau (souvent gratuite à quelques $/mois selon le FAI), ou VPN si plusieurs personnes se connectent depuis des lieux différents | ~5 $/utilisateur/mois pour un VPN d'équipe type Tailscale ; variable pour une IP fixe | 🟡 À décider avant la mise en production |

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

## 4. Paiement

**FedaPay est techniquement intégré et testé** (voir `CLAUDE.md` §7) — les clés utilisées à ce
jour sont des clés **sandbox** (bac à sable, aucun vrai argent ne circule). Il reste une seule
étape avant le lancement réel : **ouvrir un vrai compte FedaPay (production)** et remplacer les
clés sandbox par les clés live via `FEDAPAY_SECRET_KEY`/`FEDAPAY_WEBHOOK_SECRET`
(jamais commitées) + `FEDAPAY_ENABLED=true` + `FEDAPAY_BASE_URL=https://api.fedapay.com`.

| Service | Pourquoi le backend en a besoin | Taux/frais (indicatif) | Priorité |
|---|---|---|---|
| **FedaPay (compte production)** | Seul moyen d'encaisser en mobile money (T-Money, Flooz) — intégration technique faite, il manque le vrai compte marchand | Pas d'abonnement fixe. Grille officielle Bénin : **1,80 %** par paiement Mobile Money, **3,60 %** par paiement carte (Visa/Mastercard) — le Togo n'a pas de grille publique séparée, mais la presse togolaise évoque une commission FedaPay Togo de **1,8 % à 2,2 %** sur le mobile money. **À faire confirmer par FedaPay au moment de l'ouverture du compte**, ces chiffres peuvent varier par opérateur (T-Money/Flooz) et changer sans préavis | 🔴 Bloquant business |
| **Stripe** | Paiement carte bancaire | **2,9 % + 0,30 $** par paiement carte réussi (tarif standard US, +1,5 % sur les cartes internationales, +1 % de conversion de devise) — **inapplicable tant que le compte marchand togolais reste bloqué**, chiffre donné uniquement pour comparaison si l'entité légale est ouverte ailleurs | 🔴 Bloqué |

> **Point bloquant connu :** un compte marchand Stripe n'est actuellement pas ouvrable pour une
> société basée au Togo. Deux pistes à trancher avec le porteur du projet : ouvrir l'entité légale
> ailleurs, ou vérifier si FedaPay peut aussi couvrir le paiement par carte. **Ne rien souscrire
> côté carte bancaire avant cette décision.**
>
> **Sources (à revérifier avant toute décision, les grilles tarifaires changent) :**
> [Grille tarifaire officielle FedaPay](https://www.fedapay.com/pricing) (Bénin, consultée le
> 26/09/2026) · [FedaPay se lance au Togo — République Togolaise](https://www.republicoftogo.com/toutes-les-rubriques/eco-finance/fedapay-se-lance-au-togo)
> · [Stripe fees explained (2026)](https://checkoutpage.com/blog/stripe-processing-fees)

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

**Déjà couvert, aucune action requise :** l'exécution automatique des tests (CI) tourne sur GitHub Actions, gratuit pour un dépôt public et avec un volume d'heures gratuit généreux même pour un dépôt privé de cette taille.

---

*Document préparé le 23 août 2026, mis à jour le 26 septembre 2026 — tarifs indicatifs, à
vérifier à la souscription.*
