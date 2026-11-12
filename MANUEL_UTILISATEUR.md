# Maplenou — Manuel utilisateur

Ce manuel explique, pas à pas, comment utiliser le **service client** et le **programme partenaire revendeur** de
Maplenou. Il s'adresse à trois publics :

| Vous êtes… | Allez à… |
|---|---|
| un **client, vendeur ou livreur** (application Maplenou) | [1. Utilisateur de l'application](#1-utilisateur-de-lapplication) |
| un **agent ou responsable du service client** | [2. Équipe du service client](#2-équipe-du-service-client) |
| le **Super Admin** de Maplenou | [3. Super Admin](#3-super-admin) |

## Adresses

| Quoi | Adresse |
|---|---|
| Application Maplenou (web) | https://maplenou-web.onrender.com |
| Back-office Maplenou (Super Admin et personnel) | https://maplenou-admin.onrender.com |
| Espace du service client (équipe) | https://maplenou-support-web.onrender.com |

> **Bon à savoir.** Les services du service client « s'endorment » après 15 minutes sans visite. Le premier accès de la
> journée peut prendre **environ une minute** : attendez, ce n'est pas une panne.

---

## 1. Utilisateur de l'application

### 1.1 Demander de l'aide au service client

1. Ouvrez l'application et connectez-vous avec votre numéro de téléphone et votre mot de passe habituels.
2. Allez dans **Profil → Aide & Support → Centre d'aide**.
3. Touchez la carte **« Contacter le service client »** : vous voyez la liste de vos demandes.
4. Touchez **« Nouvelle demande »** et remplissez :
   - **Sujet de la demande** : la catégorie (livraison, paiement, commande…) ;
   - **Titre** : une phrase courte ;
   - **Décrivez votre problème** : donnez le maximum de détails (10 caractères minimum) ;
   - **Numéro de commande** (facultatif) : par exemple `CMD-000123`. Il doit être l'une de **vos** commandes ;
   - **C'est urgent** : seulement pour une livraison bloquée ou un paiement débité sans commande.
5. Touchez **« Envoyer ma demande »**. Votre demande reçoit un numéro (par exemple `TKT-2026-00002`).

**Raccourci depuis une commande** : dans le suivi d'une commande, **« Signaler un problème »** ouvre la même demande avec
le numéro de commande déjà rempli.

### 1.2 Suivre et répondre

- La liste affiche pour chaque demande son numéro, son titre, son **statut** (Nouveau, Pris en charge, En cours, En
  attente de votre réponse, Résolu, Clôturé) et la date de dernière mise à jour.
- Touchez une demande pour lire la conversation. Vos messages sont à droite en vert, ceux de l'équipe à gauche.
- Écrivez dans la barre du bas puis touchez la flèche pour répondre. Si la connexion coupe, vous pouvez renvoyer : le
  message n'apparaît **qu'une seule fois**.
- Le statut **« En attente de votre réponse »** veut dire que l'équipe attend une information de votre part.
- La cloche en haut de l'écran ouvre vos **notifications** (réponse de l'équipe, demande résolue…). Touchez-en une pour
  ouvrir la demande.

### 1.3 Quand la demande est résolue

- **« Le problème persiste : rouvrir »** : explique pourquoi ; l'équipe examine votre demande de réouverture.
- **« Donner mon avis »** : une note de 1 à 5 étoiles et un commentaire facultatif.

### 1.4 Programme partenaire revendeur

Gagnez de l'argent en recommandant des produits.

1. **Devenir partenaire** : Profil → **Programme partenaire** → lisez les conditions → cochez la case → **Envoyer ma
   demande**. L'équipe examine votre demande ; vous retrouvez l'écran quand elle est décidée.
2. **Choisir un produit** : onglet **Produits**. Chaque produit éligible (prix final d'au moins 2 500 FCFA) indique ce que
   vous gagnez **par unité vendue**.
3. **Partager** : touchez l'icône de partage ou de copie. Votre lien personnel s'ouvre sur la fiche du produit.
4. **Suivre vos gains** : onglet **Gains** — vos ventes, leur statut et des statistiques sur 7, 30, 90 jours ou une période
   choisie.
5. **Retirer votre argent** : onglet **Portefeuille** → **Retirer**. Minimum **3 000 FCFA**, par **Mix by Yas** ou
   **Moov Africa**. Des frais de **3 %** sont prélevés **uniquement quand le retrait est payé**.

Règles à connaître :
- La commission est créditée **après la livraison** du produit, jamais avant.
- Le dernier lien cliqué dans les 30 jours est celui qui compte.
- Vos propres achats ne rapportent rien.
- Un seul retrait peut être en cours à la fois.

---

## 2. Équipe du service client

### 2.1 Se connecter

1. Votre compte doit avoir été créé par le Super Admin dans le back-office Maplenou (voir [3.1](#31-créer-un-agent-du-service-client)).
2. **Première connexion obligatoire, dans le back-office Maplenou** : connectez-vous sur
   https://maplenou-admin.onrender.com avec le mot de passe provisoire, **changez-le**, puis **activez la vérification en
   deux étapes** (MFA).
3. Allez ensuite sur https://maplenou-support-web.onrender.com et connectez-vous avec votre **numéro de téléphone** (pas
   l'email) et votre nouveau mot de passe, puis le code à 6 chiffres.

> Tant que la première connexion n'est pas terminée dans le back-office, le service client **refuse** l'accès et vous le dit.

### 2.2 Traiter les demandes

Votre espace s'ouvre sur le tableau de bord des tickets. En résumé :

1. **Prendre un ticket** : dans la file des tickets ouverts, ouvrez-en un et attribuez-le-vous.
2. **Répondre au client** : un message public, visible dans son application.
3. **Notes internes** : visibles uniquement par l'équipe, jamais par le client.
4. **Changer le statut** : en cours, en attente du client, résolu.
5. **Escalader** un ticket difficile à un responsable.
6. **Demandes de réouverture** : un client peut demander de rouvrir un ticket résolu ; vous acceptez ou refusez.
7. **Délais** : chaque ticket a un délai de première réponse et de résolution, comptés 24 h/24 et 7 j/7. Les tickets en
   retard ressortent en priorité.

Le **responsable** voit en plus le tableau de bord complet et les rapports ; l'**administrateur** gère la page **Agents**
(rôles, catégories, modèles de réponse, calendrier).

Le guide détaillé du service client est disponible sur https://maplenou-support-help.onrender.com.

---

## 3. Super Admin

### 3.1 Créer un agent du service client

1. Back-office → **Personnel**.
2. **Créer un compte du personnel** : nom, email, téléphone, rôle **« Agent support et retraits »**. Un **mot de passe
   provisoire** s'affiche **une seule fois** : notez-le et transmettez-le de façon sécurisée.
3. L'agent termine sa première connexion dans le back-office (mot de passe + MFA), puis se connecte à l'espace du service
   client avec son **numéro de téléphone** (voir [2.1](#21-se-connecter)).
4. **Rôle dans le service client** : dans la page Personnel, colonne **« Service client »**, choisissez **Agent**,
   **Responsable** ou **Administrateur**. Le rôle s'applique à la **prochaine connexion** de la personne. « Aucun accès »
   retire un rôle donné à la main.

Un compte « Agent support » est **automatiquement agent** du service client. Votre propre compte Super Admin est
**automatiquement administrateur** du service client.

### 3.2 Voir ce que fait le service client

Dans le menu du back-office, groupe **« Service client »** :
- **Supervision** (lecture seule) : tickets ouverts, tickets sans agent, délais dépassés, activité récente, équipe et
  rôles. Une **pastille rouge** sur ce menu indique les tickets sans agent.
- **Ouvrir le service client ↗** : ouvre l'espace du personnel dans un nouvel onglet, **déjà connecté**, sans mot de passe.

Le service client garde sa propre administration (agents, rôles, catégories) : vous la supervisez, vous ne la remplacez pas.

### 3.3 Programme partenaire — administration

Dans le menu, groupe **« Partenaires revendeurs »** :

| Page | À quoi elle sert |
|---|---|
| **Partenaires** | Approuver ou refuser une demande (motif obligatoire pour un refus), suspendre, réactiver, désactiver ; voir le portefeuille ; **régulariser** un solde (Super Admin, avec motif). |
| **Commissions partenaires** | Une ligne par unité vendue : prix, commission Maplenou, part du partenaire, statut. |
| **Retraits partenaires** | Même circuit que les autres retraits (ci-dessous). |
| **Paramètres du programme** | Part du partenaire, prix minimum, seuils et taux de commission, durée d'attribution, retrait minimum, frais. Chaque modification crée une **nouvelle version** avec un motif obligatoire ; elle ne touche que les **nouvelles commandes**. |

**Circuit d'un retrait** : l'agent support **vérifie** et **prépare** le dossier, puis le **soumet** ; le Super Admin
**approuve** (le code MFA est redemandé) ou **rejette** (motif obligatoire), puis marque **payé** (avec la référence du
virement). Un rejet rend l'argent disponible au partenaire. Les frais de 3 % ne sont pris qu'au paiement.

---

## 4. Problèmes fréquents

| Problème | Que faire |
|---|---|
| L'écran du service client met longtemps à s'ouvrir | Attendez environ une minute (service endormi), puis touchez **Réessayer**. |
| « Terminez d'abord votre première connexion… » | Connectez-vous d'abord dans le back-office Maplenou : changez le mot de passe et activez la MFA. |
| Un agent ne peut pas se connecter à l'espace du service client | Il doit utiliser son **numéro de téléphone**, pas l'email, et avoir terminé sa première connexion. |
| Le rôle donné ne change rien | Il s'applique à la **prochaine connexion** : demandez à la personne de se déconnecter puis de se reconnecter. |
| « Commande introuvable dans vos références » | Le numéro de commande doit être l'une de **vos** commandes. Laissez le champ vide sinon. |
| Un retrait est refusé | Vérifiez le minimum (3 000 FCFA), votre solde disponible et qu'aucun autre retrait n'est en cours. |
| Erreur à la connexion de l'application en local | Vérifiez que Redis (Memurai) est démarré. |
