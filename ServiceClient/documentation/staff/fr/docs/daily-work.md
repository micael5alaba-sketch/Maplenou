# Travail quotidien

L'espace support s'organise autour d'un menu latéral. La section <span class="ui">Support</span> contient <span class="ui">Tableau de bord</span>, <span class="ui">File d'attente</span>, <span class="ui">Mes tickets</span> et <span class="ui">Notifications</span>. Les responsables et administrateurs voient en plus la section <span class="ui">Administration</span> (<span class="ui">Rapports</span>, <span class="ui">Agents</span>, <span class="ui">Catégories</span>, <span class="ui">Calendrier SLA</span>). Des pastilles indiquent le nombre de tickets ouverts (<span class="ui">File d'attente</span>), vos tickets ouverts (<span class="ui">Mes tickets</span>) et vos notifications non lues.

## Le tableau de bord

![Tableau de bord d'un agent](images/dashboard.jpg)

*Repères de la capture :*

1. Les compteurs : tickets ouverts, non attribués, en cours, en attente du client, résolus aujourd'hui, SLA dépassé, satisfaction. Chacun ouvre la file filtrée.
2. <span class="ui">Tickets ouverts par priorité</span> (puis par catégorie et les tickets récents).
3. <span class="ui">File d'attente</span> : tous les tickets, avec le nombre de tickets à traiter.
4. <span class="ui">Mes tickets</span> : les tickets qui vous sont attribués.

Le <span class="ui">Tableau de bord</span> donne la charge de travail de **toute l'équipe** (et non la vôtre seule). Chaque tuile est cliquable et ouvre la file d'attente déjà filtrée.

| Tuile | Ce qu'elle compte | Clic |
| --- | --- | --- |
| <span class="ui">Tickets ouverts</span> | Tickets <span class="ui">Nouveau</span>, <span class="ui">Attribué</span>, <span class="ui">En cours</span> ou <span class="ui">En attente du client</span>. La note « N nouveaux » donne le nombre de tickets <span class="ui">Nouveau</span>. | File filtrée sur <span class="ui">Ouverts (tous)</span> |
| <span class="ui">Non attribués</span> | Tickets ouverts que personne n'a pris (« à prendre en charge »). | File filtrée sur <span class="ui">Non attribué</span> |
| <span class="ui">En cours</span> | Tickets au statut <span class="ui">En cours</span>. | File filtrée sur ce statut |
| <span class="ui">En attente client</span> | Tickets au statut <span class="ui">En attente du client</span>. | File filtrée sur ce statut |
| <span class="ui">Résolus aujourd'hui</span> | Tickets marqués résolus depuis minuit (heure de Lomé). | File filtrée sur <span class="ui">Résolu</span> (tous les résolus) |
| <span class="ui">SLA dépassé</span> | Tickets ouverts dont l'échéance de première réponse ou de résolution est passée. La note « N escaladés » donne le nombre de tickets ouverts escaladés. | File filtrée sur <span class="ui">SLA dépassé</span> |

Sous les tuiles :

- <span class="ui">Tickets ouverts par priorité</span> : répartition des tickets ouverts entre <span class="ui">Critique</span>, <span class="ui">Élevée</span>, <span class="ui">Moyenne</span> et <span class="ui">Basse</span> ;
- <span class="ui">Tickets ouverts par catégorie</span> : répartition des tickets ouverts par catégorie ;
- <span class="ui">Tickets récents</span> : les cinq derniers tickets ouverts créés, avec leur statut et un badge SLA lorsqu'ils ne sont plus <span class="ui">Dans les délais</span>. <span class="ui">Tout voir</span> ouvre la file d'attente.

Le bouton <span class="ui">Ouvrir la file d'attente</span> en haut de l'écran mène directement à la file.

## La file d'attente

La <span class="ui">File d'attente</span> liste tous les tickets visibles par le support, 20 par page, du plus récent au plus ancien par défaut. Les colonnes sont <span class="ui">N°</span>, <span class="ui">Sujet</span> (avec le badge <span class="ui">Escaladé</span> le cas échéant), <span class="ui">Demandeur</span> (avec son rôle), <span class="ui">Catégorie</span>, <span class="ui">Statut</span>, <span class="ui">Priorité</span>, <span class="ui">SLA</span>, <span class="ui">Attribué à</span> et <span class="ui">Créé le</span>. Cliquez sur le numéro ou le sujet pour ouvrir le ticket.

La colonne <span class="ui">SLA</span> affiche l'état le plus défavorable entre la première réponse et la résolution (voir [SLA et calendrier](sla.md)).

## Rechercher et filtrer dans la file

![File d'attente (responsable support)](images/queue.jpg)

*Repères de la capture :*

1. <span class="ui">Rechercher</span> : un numéro complet ou une partie du numéro ou du sujet.
2. Les filtres : <span class="ui">Catégorie</span>, <span class="ui">Statut</span>, <span class="ui">Priorité</span>, <span class="ui">Attribué à</span>, <span class="ui">Rôle du demandeur</span>.
3. <span class="ui">SLA dépassé</span> et <span class="ui">Escaladés</span> : seulement les tickets en retard ou escaladés.
4. <span class="ui">Exporter (CSV)</span> : la liste filtrée dans un fichier (responsables et administrateurs).
5. Une ligne : numéro, sujet, demandeur, catégorie, statut, priorité, état SLA, agent et date. Cliquez sur le numéro ou le sujet pour ouvrir le ticket.

| Contrôle | Utilisation |
| --- | --- |
| <span class="ui">Rechercher</span> | Tapez un numéro complet (par exemple `TKT-2026-00042`) pour retrouver exactement ce ticket, ou une partie du numéro ou du sujet, puis validez. Un ticket importé de l'ancien support se retrouve aussi par son ancien numéro (`TKT-2026-000017` : 6 chiffres, voir [Anciens tickets Maplenou](administration.md#anciens-tickets-maplenou)). |
| <span class="ui">Catégorie</span> | Une catégorie de ticket. |
| <span class="ui">Statut</span> | Un statut précis, ou <span class="ui">Ouverts (tous)</span> pour tous les tickets non résolus et non fermés. |
| <span class="ui">Priorité</span> | <span class="ui">Critique</span>, <span class="ui">Élevée</span>, <span class="ui">Moyenne</span> ou <span class="ui">Basse</span>. |
| <span class="ui">Attribué à</span> | <span class="ui">Moi</span>, <span class="ui">Non attribué</span> ou un agent nommé. |
| <span class="ui">Rôle du demandeur</span> | <span class="ui">Client</span>, <span class="ui">Vendeur</span> ou <span class="ui">Livreur</span>. |
| <span class="ui">SLA dépassé</span> (case) | Uniquement les tickets ouverts dont une échéance SLA est passée. |
| <span class="ui">Escaladés</span> (case) | Uniquement les tickets escaladés. |
| <span class="ui">Tri</span> | <span class="ui">Plus récents</span>, <span class="ui">Plus anciens</span>, <span class="ui">Priorité</span> (Critique d'abord) ou <span class="ui">Dernière activité</span>. |
| <span class="ui">Réinitialiser</span> | Efface tous les filtres. |

Les filtres combinés s'additionnent (par exemple <span class="ui">Non attribué</span> + <span class="ui">Priorité</span> <span class="ui">Élevée</span>). Si rien ne correspond, l'écran affiche « Aucun ticket ne correspond à ces filtres ».

!!! tip "Enregistrer une vue"
    Les filtres sont inscrits dans l'adresse de la page : ajoutez une vue filtrée à vos favoris ou envoyez son lien à un collègue pour qu'il voie la même liste.

## Mes tickets

<span class="ui">Mes tickets</span> affiche la même liste que la file d'attente, limitée aux tickets qui vous sont attribués. Tous les filtres sont disponibles sauf <span class="ui">Attribué à</span>. Pour ne voir que le travail restant, choisissez <span class="ui">Statut</span> → <span class="ui">Ouverts (tous)</span>.

## Les notifications

L'écran <span class="ui">Notifications</span> regroupe les notifications dans l'application. Les onglets <span class="ui">Toutes</span> et <span class="ui">Non lues</span> filtrent la liste ; <span class="ui">Tout marquer comme lu</span> vide le compteur. Cliquer sur une notification ouvre le ticket et la marque comme lue.

| Notification | Qui la reçoit |
| --- | --- |
| Nouveau ticket | Les responsables support actifs, à chaque création de ticket. |
| Ticket attribué | L'agent à qui le ticket vient d'être attribué (sauf s'il se l'est attribué lui-même). |
| Réponse du demandeur | L'agent attribué ; si le ticket n'est pas attribué, les responsables support actifs. |
| Demande de réouverture | L'agent attribué ; si le ticket n'est pas attribué, les responsables support actifs. |
| Ticket escaladé | Les responsables support actifs. |

Vous n'êtes jamais notifié de vos propres actions. Les notifications sont envoyées après l'enregistrement de l'action : un incident de notification n'annule jamais une action réussie sur un ticket (BR-020). Pensez donc à consulter la file d'attente, même sans notification.

## Les notifications push dans le navigateur

Pour être alerté sur votre ordinateur même quand l'espace support n'est pas affiché, activez les notifications push dans votre navigateur :

1. Ouvrez <span class="ui">Notifications</span> dans le menu latéral.
2. Dans la carte <span class="ui">Notifications push</span>, cliquez sur <span class="ui">Activer les notifications push</span>, puis autorisez les notifications quand le navigateur le demande.
3. Cliquez sur <span class="ui">Envoyer une notification de test</span> : une notification « Notification de test » doit apparaître après quelques secondes.

Cliquer sur une notification push ouvre le ticket concerné. Les textes des notifications push sont en français. <span class="ui">Désactiver</span> arrête les notifications push sur cet appareil ; la déconnexion les arrête aussi pour ce navigateur, ce qui compte sur un poste partagé.

Si la carte indique que les notifications sont bloquées, cliquez sur l'icône à gauche de l'adresse du site, autorisez les notifications, puis rechargez la page. Si elle indique que le navigateur ne les prend pas en charge ou qu'elles ne sont pas configurées, l'écran <span class="ui">Notifications</span> reste votre référence. Les applications mobiles demandent elles-mêmes l'autorisation d'afficher des notifications.

## Routine quotidienne conseillée

Cette routine est une suggestion d'organisation, pas une règle.

1. **Ouvrir le tableau de bord** et regarder les tuiles <span class="ui">SLA dépassé</span> et <span class="ui">Non attribués</span>.
2. **Traiter les urgences** : file filtrée sur <span class="ui">SLA dépassé</span>, puis tri par <span class="ui">Priorité</span>.
3. **Lire les notifications non lues**, en particulier les réponses des demandeurs et les demandes de réouverture.
4. **Avancer sur <span class="ui">Mes tickets</span>** (statut <span class="ui">Ouverts (tous)</span>, tri <span class="ui">Priorité</span>) : répondre, demander les informations manquantes, marquer résolu ce qui est réglé.
5. **Prendre des tickets non attribués** selon votre charge, en commençant par les plus prioritaires et les plus anciens.
6. **Avant de partir**, laisser une note interne sur les tickets en cours qui demandent un suivi.

Pour les responsables : surveiller aussi la case <span class="ui">Escaladés</span> et la charge de chaque agent (colonne <span class="ui">Tickets ouverts</span> de l'écran <span class="ui">Agents</span>), et réattribuer si besoin.
