# Traiter un ticket

L'écran d'un ticket réunit tout ce qu'il faut pour le traiter sans changer d'outil : à gauche le demandeur et le contexte Maplenou (commande, paiement, livraison…), au centre les échanges, à droite les actions et le SLA. Seuls les boutons que vous avez le droit d'utiliser sur ce ticket sont affichés.

## Ouvrir un ticket et lire l'en-tête

![Espace de travail d'un ticket](images/ticket-workspace.jpg)

*Repères de la capture :*

1. Les onglets <span class="ui">Conversation</span>, <span class="ui">Notes internes</span>, <span class="ui">Historique</span> et <span class="ui">Pièces jointes</span>.
2. <span class="ui">Insérer une réponse type</span> : un texte prêt à adapter.
3. <span class="ui">Brouillon par l'assistant</span> : une proposition de réponse à relire (jamais envoyée sans vous).
4. <span class="ui">Actions sur le ticket</span> : statut, priorité, catégorie, escalade.
5. <span class="ui">Actions sur Maplenou</span> : demande de retour, remise à disposition de la course, message au livreur.
6. Le contexte Maplenou : <span class="ui">Commande</span>, puis paiement, livraison et vendeur.

Ouvrez un ticket depuis la <span class="ui">File d'attente</span>, <span class="ui">Mes tickets</span>, le <span class="ui">Tableau de bord</span> ou une notification. L'en-tête affiche :

- le fil d'Ariane <span class="ui">Tickets</span> / numéro du ticket (format `TKT-année-numéro`, jamais réutilisé) ;
- le badge de statut, le badge de priorité et, s'il y a lieu, le badge <span class="ui">Escaladé</span> ;
- le sujet, puis la catégorie, la date de création et le nom du demandeur ;
- le badge <span class="ui">Urgence signalée</span> si le demandeur a coché l'urgence à la création. C'est une information pour vous aider à fixer la priorité : elle ne met **jamais** le ticket en <span class="ui">Critique</span> automatiquement (BR-037).

Si le ticket est attribué à un collègue, l'encadré <span class="ui">Lecture seule</span> rappelle que seul l'agent attribué ou un responsable peut le modifier.

## Consulter le demandeur et le contexte Maplenou

La colonne de gauche est réservée au support.

| Panneau | Informations |
| --- | --- |
| <span class="ui">Informations demandeur</span> | Nom, e-mail, <span class="ui">Profil</span> (Client, Vendeur ou Livreur), <span class="ui">Téléphone</span>, <span class="ui">Connu depuis</span>, nombre de <span class="ui">Tickets</span> du demandeur. |
| <span class="ui">Commande</span> | <span class="ui">N°</span>, <span class="ui">Date</span>, <span class="ui">Montant</span>, <span class="ui">Statut</span>, <span class="ui">Vendeur</span>. |
| <span class="ui">Paiement</span> | <span class="ui">N°</span>, <span class="ui">Statut</span>, <span class="ui">Prestataire</span> (FedaPay, Stripe), <span class="ui">Commission</span>, <span class="ui">Reversement</span> (statut et montant). |
| <span class="ui">Livraison</span> | <span class="ui">N°</span>, <span class="ui">Livreur</span>, <span class="ui">Statut</span>, <span class="ui">Destination</span>, <span class="ui">Créneau</span>, <span class="ui">Mise à jour</span>. |
| <span class="ui">Vendeur</span> | <span class="ui">Boutique</span>, <span class="ui">Compte</span> (statut du compte), <span class="ui">Solde</span>. |
| <span class="ui">Remboursement</span> | <span class="ui">N°</span>, <span class="ui">Statut</span>, <span class="ui">Montant</span>. |

!!! note "Fonctionnement de la commission (depuis novembre 2026)"
    La commission Maplenou est **comprise dans le prix affiché à l'acheteur** : 5 % du prix de base du vendeur
    (catégorie Standard) ou 3 % (catégorie Prix élevés), arrondie au FCFA par unité, livraison exclue. L'acheteur
    ne paie aucun supplément et le vendeur reçoit son prix de base : la commission n'est jamais retirée du
    reversement. Les commandes passées avant ce changement gardent l'ancien modèle (25 % / 22,5 % prélevés sur le vendeur).

    - Exemple (Standard) : prix de base 10 000 FCFA + commission 500 FCFA = **10 500 FCFA** affichés, livraison en plus.
    - **Frais mensuel vendeur** : 500 FCFA (Standard) ou 2 000 FCFA (Prix élevés). Les 3 premières ventes finalisées
      sont sans frais ; à partir de la 4ᵉ, **un seul** frais par mois, seulement pour un mois avec au moins une vente
      finalisée. Il est déduit du solde disponible **avant** le versement (8 000 FCFA disponibles − 500 FCFA = 7 500 FCFA
      versés) ; si le solde ne suffit pas, le reste demeure dû pour le prochain versement. Un même frais n'est jamais
      prélevé deux fois.
    - Une commande **annulée ou remboursée** ne compte pas comme vente ; la commission comprise dans son prix est
      remboursée avec elle.
    - Le vendeur choisit sa catégorie à l'inscription ; Maplenou peut demander une correction si elle ne correspond
      pas aux produits. Un changement approuvé s'applique au 1er du mois suivant, aux nouvelles commandes seulement.
    - Le vendeur est notifié à la 4ᵉ vente (début du frais), à chaque déduction et à chaque remboursement comportant
      une commission. Dans l'application, il voit le résumé de ses frais (catégorie, montant, mois, statut),
      l'historique des déductions et le montant net estimé avant un retrait.

Les panneaux n'apparaissent que si le ticket est lié à une référence. À la création, le demandeur peut lier une commande ou une livraison qui le concerne (un client : une commande qu'il a achetée ; un vendeur : une commande qu'il a vendue ; un livreur : une livraison qui lui est confiée). Les références de paiement, livraison, vendeur et remboursement sont ensuite déduites automatiquement de la commande.

!!! info "Service momentanément indisponible"
    Chaque panneau est chargé séparément. Si un service Maplenou ne répond pas, le panneau affiche « Service momentanément indisponible. » ; le reste du ticket reste utilisable. « Référence introuvable. » signifie que la référence liée n'a pas été retrouvée.

Les actions dans les autres modules (rembourser, reprogrammer une livraison, contacter le livreur depuis l'application) ne sont pas disponibles depuis le ticket : aucune règle approuvée ne les définit encore.

## Prendre en charge ou attribuer le ticket

Le panneau <span class="ui">Actions sur le ticket</span> indique <span class="ui">Attribué à :</span> suivi du nom de l'agent ou de <span class="ui">Non attribué</span>.

- **Agent** : sur un ticket non attribué, cliquez sur <span class="ui">Prendre en charge</span>. Le ticket vous est attribué et passe de <span class="ui">Nouveau</span> à <span class="ui">Attribué</span>.
- **Responsable** : <span class="ui">Prendre en charge</span> (ticket non attribué) ou <span class="ui">M'attribuer ce ticket</span> (ticket déjà attribué) ; la liste <span class="ui">Attribuer à un agent</span> → <span class="ui">Choisir un agent…</span> attribue le ticket à un agent ou responsable actif. Chaque option indique la charge de l'agent, sous la forme « Nom (3 ouverts) ».
- **Administrateur** : peut attribuer à un agent avec <span class="ui">Attribuer à un agent</span>, mais ne peut pas se l'attribuer.

Chaque attribution ou réattribution est enregistrée dans l'historique (BR-013) et l'agent qui reçoit le ticket est notifié.

## Ajuster la priorité et la catégorie

Dans <span class="ui">Actions sur le ticket</span> :

- <span class="ui">Priorité</span> : choisissez <span class="ui">Basse</span>, <span class="ui">Moyenne</span>, <span class="ui">Élevée</span> ou <span class="ui">Critique</span>. Le changement est immédiat, sauf pour <span class="ui">Critique</span> qui ouvre la fenêtre <span class="ui">Passer en priorité Critique ?</span> : indiquez un <span class="ui">Motif</span> (le champ suggère « Ex. fraude présumée »), puis <span class="ui">Confirmer Critique</span>. Tout changement de priorité recalcule le SLA (voir [SLA et calendrier](sla.md)).
- <span class="ui">Catégorie</span> : choisissez une catégorie active ; le message « Catégorie modifiée. » confirme.

Les deux changements sont inscrits dans l'historique avec l'ancienne et la nouvelle valeur (BR-008).

## Répondre dans la conversation

L'onglet <span class="ui">Conversation</span> contient les messages visibles par le demandeur, dont sa description initiale. Écrivez dans le champ <span class="ui">Répondre au client…</span> puis cliquez sur <span class="ui">Envoyer</span>.

- Votre première réponse visible enregistre la **première réponse** du SLA ; si le ticket est <span class="ui">Attribué</span>, il passe automatiquement <span class="ui">En cours</span>.
- Le demandeur est notifié de chaque réponse.
- En cas d'échec d'envoi, un message propose <span class="ui">Réessayer</span> : le message n'est jamais enregistré deux fois.
- Un message compte au plus 5 000 caractères.

Pour répondre ou changer le statut d'un ticket non attribué, prenez-le d'abord en charge.

## Écrire une note interne

L'onglet <span class="ui">Notes internes</span> sert aux échanges entre membres du support : investigation, passation, demande d'avis. Écrivez dans le champ <span class="ui">Note visible uniquement par le support…</span> puis cliquez sur <span class="ui">Ajouter la note</span>.

!!! danger "Jamais visible par le demandeur"
    Les notes internes et leurs pièces jointes ne sont **jamais** visibles par le demandeur (BR-016), ni dans l'application, ni dans les notifications. À l'inverse, tout ce que vous écrivez dans l'onglet <span class="ui">Conversation</span> est visible par lui : vérifiez l'onglet actif avant d'envoyer.

Une note interne ne compte pas comme première réponse pour le SLA.

## Joindre des fichiers

Sous le champ de saisie (conversation ou note), <span class="ui">+ Pièce jointe</span> ajoute des fichiers au message :

- formats acceptés : JPG, JPEG, PNG, WEBP, PDF ;
- 10 Mo maximum par fichier, 5 fichiers maximum par message, 50 Mo maximum au total par ticket (DEC-003).

Le type est vérifié sur le contenu réel du fichier : un fichier renommé est refusé. L'onglet <span class="ui">Pièces jointes</span> liste tous les fichiers du ticket avec leur taille, leur auteur et leur date ; ceux qui proviennent d'une note interne portent la mention <span class="ui">Note interne</span>. Cliquez sur un fichier pour le télécharger.

## Changer le statut

La zone <span class="ui">Changer le statut</span> propose uniquement les changements autorisés depuis le statut actuel :

| Bouton | Effet |
| --- | --- |
| <span class="ui">Démarrer le traitement</span> | <span class="ui">Attribué</span> → <span class="ui">En cours</span> |
| <span class="ui">Demander des informations</span> | → <span class="ui">En attente du client</span> ; le demandeur est notifié que le support a besoin d'informations |
| <span class="ui">Reprendre le traitement</span> | <span class="ui">En attente du client</span> → <span class="ui">En cours</span> |
| <span class="ui">Marquer résolu</span> | → <span class="ui">Résolu</span>, après confirmation dans <span class="ui">Marquer le ticket comme résolu ?</span> |
| <span class="ui">Clôturer le ticket</span> | <span class="ui">Résolu</span> → <span class="ui">Fermé</span>, après confirmation dans <span class="ui">Clôturer le ticket ?</span> |

Les fenêtres de confirmation proposent un <span class="ui">Commentaire (historique, optionnel)</span>, enregistré dans l'historique. Le détail des transitions est dans [Statuts et transitions](statuses-and-transitions.md).

!!! warning "Clôturer est définitif"
    Un ticket <span class="ui">Fermé</span> ne peut plus être rouvert par le demandeur : il devra créer un nouveau ticket (BR-052, BR-053). Ne clôturez qu'un ticket résolu dont le demandeur n'a plus besoin.

## Escalader

<span class="ui">Escalader au responsable</span> signale le ticket aux responsables support. Le motif est obligatoire. Voir [Escalade](escalation.md).

## Consulter l'historique

L'onglet <span class="ui">Historique</span> retrace chaque événement important avec son auteur et sa date : <span class="ui">Ticket créé</span>, <span class="ui">Statut modifié</span>, <span class="ui">Priorité modifiée</span>, <span class="ui">Catégorie modifiée</span>, <span class="ui">Attribué</span>, <span class="ui">Réattribué</span>, <span class="ui">Message envoyé</span>, <span class="ui">Note interne ajoutée</span>, <span class="ui">Pièce jointe ajoutée</span>, <span class="ui">Ticket escaladé</span>, <span class="ui">Réouverture demandée</span>, <span class="ui">Réouverture acceptée</span>, <span class="ui">Réouverture refusée</span>, <span class="ui">SLA recalculé</span>. Les changements affichent l'ancienne et la nouvelle valeur.

L'historique ne peut être ni modifié ni supprimé (BR-018). Le demandeur en voit une version réduite : création, changements de statut, de priorité et de catégorie, pièces jointes des messages visibles et étapes de réouverture (avec le motif et la note de décision). Il ne voit pas les attributions, les notes internes, les escalades, les recalculs SLA ni vos commentaires et motifs.

## Lire le panneau SLA

Sous les actions, le panneau <span class="ui">SLA</span> affiche l'état de la <span class="ui">Première réponse</span> et de la <span class="ui">Résolution</span>, chacune avec son échéance, et la date de réponse ou de résolution une fois atteinte. Après une réouverture, une ligne dépliable liste les périodes de résolution. Voir [SLA et calendrier](sla.md).

## Utiliser une réponse type ou un brouillon de l'assistant

Sous la zone de réponse de l'onglet <span class="ui">Conversation</span> :

- la liste <span class="ui">Réponses types</span> insère un texte préparé par les responsables (voir [Administration](administration.md#reponses-types-et-textes-des-notifications)) ;
- le bouton <span class="ui">✨ Brouillon par l'assistant</span> (si l'aide IA est activée) rédige une réponse à partir du guide utilisateur, dans votre langue, signée de votre nom. Les titres des sections utilisées s'affichent à côté.

Dans les deux cas le texte est seulement **inséré** : relisez-le, adaptez-le au demandeur, puis cliquez sur <span class="ui">Envoyer</span>. L'assistant ne reçoit que le sujet, la description et les messages publics du ticket — jamais le nom, le téléphone, l'e-mail ni les données de paiement.

## Suggestion de catégorie et de priorité

Quand l'aide IA est activée, l'assistant lit chaque nouveau ticket et propose une catégorie et une priorité dans la carte <span class="ui">✨ Suggestion de l'assistant</span>, avec un indice de confiance et une courte raison. Cliquez sur <span class="ui">Appliquer</span> pour changer la catégorie et la priorité (inscrit dans l'historique), ou sur <span class="ui">Ignorer</span>. Rien n'est appliqué sans vous ; le demandeur ne voit jamais la suggestion.

## Agir sur Maplenou depuis le ticket

La carte <span class="ui">Actions sur Maplenou</span> apparaît quand le ticket vous est attribué (ou pour un responsable) et qu'il est lié à une commande ou à une livraison :

| Bouton | Ce que fait Maplenou |
| --- | --- |
| <span class="ui">Demander un retour / remboursement</span> | ouvre une demande de retour au nom de l'acheteur (motif d'au moins 10 caractères, état du produit, boutique concernée si la commande en a plusieurs) ; les règles habituelles s'appliquent : 30 jours après la livraison, une seule demande par boutique ; le vendeur décide ensuite |
| <span class="ui">Remettre la course à disposition</span> | retire la course au livreur actuel pour qu'un autre la prenne ; impossible si le livreur est déjà en route |
| <span class="ui">Écrire au livreur</span> | envoie votre message dans la conversation support du livreur, dans l'app Maplenou, avec le numéro du ticket et votre nom |

Maplenou peut refuser une demande : son message s'affiche (par exemple « Le délai de retour de 30 jours après livraison est dépassé »). Acceptée ou refusée, chaque demande est listée sous <span class="ui">Actions envoyées</span> et dans l'historique du ticket.

## Si l'action échoue

- « Le ticket a été modifié entre-temps. Rechargez puis réessayez. » : un collègue a modifié le ticket en même temps que vous. Rechargez la page et refaites l'action si elle est toujours utile.
- « Transition impossible : … » : le changement de statut demandé n'est pas autorisé depuis le statut actuel.
- « Le ticket doit être attribué à un agent support actif. » : l'agent choisi est désactivé ou n'est pas attribuable.
