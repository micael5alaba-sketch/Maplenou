# Rôles et accès

L'accès à un ticket dépend de trois éléments : votre rôle, votre lien avec le ticket (attribué à vous, non attribué, attribué à un collègue) et l'action demandée. Avoir un rôle support ne donne pas tous les droits sur tous les tickets : chaque action est vérifiée séparément, et tout ce qui n'est pas explicitement autorisé est refusé (BR-071, BR-074, BR-076).

## Les trois rôles du support

| Rôle | Libellé dans l'application | Mission |
| --- | --- | --- |
| Agent support | <span class="ui">Agent support</span> | Prend en charge et résout les tickets. Pleine autorité sur les tickets qui lui sont attribués. |
| Responsable support | <span class="ui">Responsable support</span> | Supervise l'équipe : peut agir sur tous les tickets, réattribuer entre agents, reçoit les escalades, consulte les rapports. |
| Administrateur | <span class="ui">Administrateur</span> | Administre le module : activation des comptes support, catégories, jours fériés du calendrier SLA. Peut agir sur tous les tickets, mais ne peut pas se voir attribuer un ticket. |

Chacun se connecte avec son compte Maplenou (même numéro de téléphone et même mot de passe que dans l'application Maplenou). Un compte Maplenou n'a pas de rôle support : un administrateur le donne par numéro de téléphone dans la section <span class="ui">Rôles support</span> de l'écran <span class="ui">Agents</span>, et il s'applique à la prochaine connexion de la personne. Les administrateurs de la plateforme Maplenou sont automatiquement <span class="ui">Administrateur</span>. Voir [Administration](administration.md#roles-support-des-comptes-maplenou).

## Les demandeurs

Les demandeurs sont les **clients** (<span class="ui">Client</span>), **vendeurs** (<span class="ui">Vendeur</span>) et **livreurs** (<span class="ui">Livreur</span>) authentifiés. Ce sont les seuls à créer des tickets depuis leur espace (BR-003). Un demandeur :

- ne voit que ses propres tickets ;
- peut écrire dans la conversation tant que son ticket n'est pas <span class="ui">Fermé</span> et joindre des fichiers ;
- choisit la priorité <span class="ui">Basse</span>, <span class="ui">Moyenne</span> ou <span class="ui">Élevée</span> et peut signaler une urgence, mais ne peut jamais choisir <span class="ui">Critique</span> (BR-036, BR-037) ;
- peut demander la réouverture de son propre ticket <span class="ui">Résolu</span>, jamais d'un ticket <span class="ui">Fermé</span> (BR-048, BR-052) ;
- voit le statut, la priorité, la catégorie, sa référence de commande ou de livraison et le prénom de l'agent qui traite son ticket ;
- ne voit jamais les notes internes, les données SLA, les détails de paiement, de vendeur ou de remboursement, ni le détail interne de l'historique.

## Tableau des droits

Ce tableau reprend la matrice d'autorisation approuvée, précisée par le fonctionnement réel de l'application. « Ses tickets » désigne les tickets attribués à l'agent ; « non attribués » les tickets que personne n'a encore pris.

| Action | Demandeur | Agent support | Responsable support | Administrateur |
| --- | :---: | :---: | :---: | :---: |
| Voir la file d'attente et les tickets non attribués | — | ✓ | ✓ | ✓ |
| Voir le ticket d'un autre agent | — | ✓ (lecture seule) | ✓ | ✓ |
| Voir les notes internes, le contexte et le SLA | — | ✓ | ✓ | ✓ |
| Prendre en charge un ticket non attribué | — | ✓ | ✓ | — (non attribuable) |
| Attribuer ou réattribuer à un agent | — | — | ✓ | ✓ |
| Répondre au demandeur (message visible) | Son ticket | Ses tickets | ✓ | ✓\* |
| Ajouter une note interne | — | Ses tickets et non attribués | ✓ | ✓\* |
| Changer la catégorie ou la priorité | — | Ses tickets et non attribués | ✓ | ✓\* |
| Passer en priorité <span class="ui">Critique</span> | — | Ses tickets et non attribués | ✓ | ✓ |
| Changer le statut, marquer résolu, clôturer | Limité\*\* | Ses tickets | ✓ | ✓\* |
| Ajouter une pièce jointe | Son ticket | Là où il peut écrire | ✓ | ✓\* |
| Escalader au responsable | — | Ses tickets et non attribués | ✓ | ✓ |
| Demander une réouverture | Son ticket résolu | — | — | — |
| Accepter ou refuser une réouverture | — | Ses tickets | ✓ | ✓\* |
| Consulter les rapports | — | — | ✓ | ✓ |
| Consulter les pages d'administration | — | — | ✓ (lecture) | ✓ |
| Activer ou désactiver un compte support, modifier les catégories et les jours fériés | — | — | — | ✓ |
| Gérer les utilisateurs et les rôles | — | — | — | ✓ (comptes dans Maplenou ; rôles support dans <span class="ui">Rôles support</span>) |

\* Capacité administrative soumise au principe du moindre privilège : un administrateur n'intervient sur un ticket que lorsque c'est nécessaire.
\*\* Le demandeur ne change pas directement le statut : sa réponse à un ticket <span class="ui">En attente du client</span> le repasse automatiquement <span class="ui">En cours</span>, et il peut demander une réouverture.

Sur un ticket <span class="ui">Fermé</span>, aucune action n'est possible, quel que soit le rôle : le ticket reste consultable.

## Ce qu'un agent peut faire selon le ticket

=== "Ticket qui m'est attribué"

    Pleine autorité opérationnelle (BR-056) : répondre, ajouter des notes internes, changer la priorité (y compris <span class="ui">Critique</span>) et la catégorie, changer le statut, marquer résolu, clôturer, escalader, accepter ou refuser une réouverture.

=== "Ticket non attribué"

    Vous pouvez le prendre avec <span class="ui">Prendre en charge</span> (BR-057). Avant de le prendre, vous pouvez déjà le trier : changer la priorité (y compris <span class="ui">Critique</span>) et la catégorie, ajouter une note interne, l'escalader. Pour **répondre au demandeur ou changer le statut, vous devez d'abord le prendre en charge**.

=== "Ticket d'un collègue"

    Vous pouvez le consulter entièrement (conversation, notes internes, historique, contexte, SLA) pour les besoins du service (BR-069), mais l'écran affiche l'encadré <span class="ui">Lecture seule</span> : seul l'agent attribué ou un responsable peut le modifier (BR-070). Vous ne pouvez pas le réattribuer (BR-058) ; si nécessaire, demandez à un responsable.

!!! note "En attente de validation"
    Le droit de tri d'un agent sur un ticket **non attribué** (priorité, catégorie, note interne, escalade avant prise en charge) est une interprétation de la règle « tickets qu'il est autorisé à traiter » (BR-056). Elle est appliquée par l'application mais attend la confirmation du responsable du projet (décision DEC-CS03-08).

## Ce que peuvent faire en plus les responsables et administrateurs

- **Responsable support** : agit sur tous les tickets, comme s'ils lui étaient attribués. Il peut se les attribuer (<span class="ui">M'attribuer ce ticket</span>, même s'ils sont déjà attribués) ou les attribuer à un agent actif avec <span class="ui">Attribuer à un agent</span> (BR-059). Il reçoit les notifications de nouveaux tickets et d'escalade. Il voit les menus <span class="ui">Rapports</span>, <span class="ui">Agents</span>, <span class="ui">Catégories</span> et <span class="ui">Calendrier SLA</span>, ces trois derniers en lecture seule.
- **Administrateur** : mêmes droits opérationnels que le responsable, sauf qu'il ne peut pas recevoir de ticket (seuls les agents et responsables actifs sont attribuables). Il est le seul à pouvoir activer ou désactiver un compte support, modifier les catégories et gérer les jours fériés (BR-065).

## Si une action vous est refusée

L'application n'affiche que les boutons autorisés pour vous sur ce ticket. Si une action est refusée malgré tout (par exemple parce qu'un responsable vient de réattribuer le ticket), un message s'affiche, comme « Vous n'êtes pas autorisé à effectuer cette action sur ce ticket. » ou « Seul un responsable support peut réattribuer un ticket. ». Rechargez la page pour voir les actions désormais disponibles. Si vous pensez avoir besoin d'un droit, adressez-vous à votre responsable support.
