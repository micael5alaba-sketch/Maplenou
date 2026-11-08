# Administration

La section <span class="ui">Administration</span> du menu latéral est réservée aux responsables support et aux administrateurs. Elle contient <span class="ui">Rapports</span> (voir [Rapports](reports.md)), <span class="ui">Agents</span>, <span class="ui">Catégories</span> et <span class="ui">Calendrier SLA</span>.

!!! info "Qui peut modifier"
    Les **responsables support** consultent ces écrans en lecture seule. Seuls les **administrateurs** peuvent modifier : activation des comptes, rôles support, import des anciens tickets, catégories, jours fériés (BR-065). Les sections <span class="ui">Rôles support</span> et <span class="ui">Anciens tickets Maplenou</span> ne s'affichent que pour eux. Les agents support n'y ont pas accès.

## Agents support : charge et activation

L'écran <span class="ui">Agents</span> (titre <span class="ui">Agents support</span>) liste les comptes support : agents, responsables et administrateurs, avec les colonnes <span class="ui">Agent</span> (nom et e-mail), <span class="ui">Rôle</span>, <span class="ui">Tickets ouverts</span> (charge actuelle) et <span class="ui">Statut</span> (<span class="ui">Actif</span> ou <span class="ui">Désactivé</span>).

Pour un administrateur, chaque ligne propose <span class="ui">Désactiver</span> ou <span class="ui">Réactiver</span>. Vous ne pouvez pas désactiver votre propre compte.

Un compte désactivé :

- ne peut plus utiliser l'espace support : toutes ses requêtes sont refusées (« Ce compte support est désactivé. ») ;
- ne peut plus recevoir de ticket et disparaît des listes <span class="ui">Attribuer à un agent</span> et <span class="ui">Attribué à</span> ;
- ne reçoit plus les notifications destinées aux responsables.

!!! warning "Réattribuer les tickets d'un agent désactivé"
    Désactiver un agent ne réattribue pas ses tickets : ils restent à son nom. Un responsable doit les réattribuer (file filtrée sur <span class="ui">Attribué à</span>, puis <span class="ui">Attribuer à un agent</span> sur chaque ticket). Faites-le avant la désactivation, tant que l'agent apparaît encore dans le filtre.

La création d'un compte support depuis cet écran n'est pas disponible : un compte apparaît dans la liste lorsque la personne utilise l'application avec un rôle support. Le rôle se donne dans la section suivante.

## Rôles support des comptes Maplenou

Le personnel du support se connecte avec son compte Maplenou : le même numéro de téléphone et le même mot de passe que dans l'application Maplenou. Un compte Maplenou n'a pas de rôle support ; à la connexion, il reçoit le profil de son compte : <span class="ui">Administrateur</span> pour un administrateur de la plateforme Maplenou, <span class="ui">Livreur</span> pour un livreur, <span class="ui">Vendeur</span> pour un vendeur dont le profil est approuvé, sinon <span class="ui">Client</span>. Un rôle donné ici l'emporte sur ce profil.

Pour donner un rôle support (administrateurs), dans la section <span class="ui">Rôles support</span> de l'écran <span class="ui">Agents</span> :

1. Saisissez le <span class="ui">Numéro de téléphone</span> du compte Maplenou, avec l'indicatif (par exemple +228 90 00 00 00).
2. Choisissez le <span class="ui">Rôle</span> : <span class="ui">Agent support</span>, <span class="ui">Responsable support</span> ou <span class="ui">Administrateur</span>.
3. Cliquez sur <span class="ui">Enregistrer</span>. Le message « Rôle enregistré pour … » confirme l'enregistrement.

Saisir un numéro déjà présent change son rôle. Un numéro mal formé est refusé avec « Numéro de téléphone invalide. ».

Le tableau liste les rôles donnés, avec les colonnes <span class="ui">Numéro de téléphone</span>, <span class="ui">Rôle</span> et <span class="ui">Attribué le</span> ; sans rôle donné, il affiche « Aucun rôle support attribué. ». Pour retirer un rôle, cliquez sur <span class="ui">Retirer</span> sur sa ligne (« Rôle retiré pour … ») : à sa prochaine connexion, la personne retrouve le profil de son compte Maplenou.

!!! warning "Le rôle s'applique à la prochaine connexion"
    Une personne déjà connectée garde son ancien rôle jusqu'à la fin de sa session (8 heures au plus) et sa prochaine connexion. Pour couper l'accès d'un membre du support tout de suite, désactivez aussi son compte dans la liste ci-dessus (<span class="ui">Désactiver</span>).

## Anciens tickets Maplenou

Avant ce service, l'application Maplenou avait son propre support, avec des tickets numérotés comme `TKT-2026-000017` (6 chiffres ; ceux du service client en ont 5, comme `TKT-2026-00042`). La section <span class="ui">Anciens tickets Maplenou</span> de l'écran <span class="ui">Agents</span> les reprend dans le service client, pour que rien ne soit perdu.

Pour importer (administrateurs) : cliquez sur <span class="ui">Importer les anciens tickets</span>. Le message « … ticket(s) importé(s), … déjà présent(s). » donne le résultat.

- **L'import peut être relancé sans risque** : un ticket déjà importé est ignoré et compté parmi les « déjà présents ».
- **Numéros** : chaque ticket importé reçoit un numéro du service client. L'ancien numéro (6 chiffres) s'affiche entre parenthèses à côté du nouveau, dans l'écran du ticket (côté support et côté demandeur), et la recherche de la file le retrouve.
- **Contenu repris** : date de création, demandeur, sujet, description, livraison concernée ; le lien d'une photo jointe devient une ligne de texte du premier message. La priorité est <span class="ui">Moyenne</span>.
- **Catégorie** : déduite des mots de l'ancienne catégorie et du sujet — « retour » ou « rembours… » → Retour / remboursement ; « livraison », « livreur » ou « colis » → Livraison ; « paiement » ou « mobile money » → Paiement ; « commande » → Commande ; « vendeur » ou « boutique » → Vendeur ; « compte », « connexion » ou « mot de passe » → Compte ; sinon Autre.
- **Statut** : un ancien ticket ouvert devient <span class="ui">Nouveau</span> ; un ticket en cours devient <span class="ui">Nouveau</span> avec une note interne indiquant qu'il est à reprendre (il n'est pas attribué) ; un ticket résolu devient <span class="ui">Résolu</span>, et ses notes de résolution deviennent une réponse visible du demandeur.
- **Aucune notification** n'est envoyée aux demandeurs.

Les échéances SLA partent de la date de création d'origine : un ancien ticket encore ouvert apparaît donc en général en <span class="ui">Délai dépassé</span>. Attribuez-les et traitez-les en priorité.

Si l'import affiche « Import impossible : renseignez MAPLENOU_CORE_URL et SUPPORT_SERVICE_TOKEN (des deux côtés). » ou indique que la plateforme Maplenou ne répond pas, le service client n'est pas encore relié à la plateforme : contactez l'équipe technique.

## Catégories de tickets

L'écran <span class="ui">Catégories</span> (titre <span class="ui">Catégories de tickets</span>) liste les sept catégories, avec les colonnes <span class="ui">Code</span>, <span class="ui">Nom</span>, <span class="ui">Description</span> et <span class="ui">Statut</span> (<span class="ui">Actif</span> ou <span class="ui">Inactif</span>).

| Code | Catégorie métier | Nom par défaut (tel qu'enregistré) |
| --- | --- | --- |
| `ORDER` | Commande | Commande |
| `PAYMENT` | Paiement | Paiement |
| `DELIVERY` | Livraison | Livraison |
| `SELLER` | Vendeur | Vendeur |
| `ACCOUNT` | Compte | Compte |
| `RETURN_REFUND` | Retour/Remboursement | Retour / remboursement |
| `OTHER` | Autre | Autre |

Les noms de catégories sont des données saisies par les administrateurs, et non des libellés de l'interface : ils s'affichent tels qu'enregistrés, dans les deux langues.

Pour modifier une catégorie (administrateurs) : cliquez sur <span class="ui">Modifier</span>, changez le <span class="ui">Nom</span> (obligatoire, 80 caractères maximum), la <span class="ui">Description</span> (255 caractères maximum) ou la case <span class="ui">Active</span>, puis <span class="ui">Enregistrer</span>.

Règles :

- **Les administrateurs ajoutent des catégories** (BR-006 modifiée, DEC-CS11-06) avec <span class="ui">Nouvelle catégorie</span> : un code en MAJUSCULES (par exemple `DIGITAL_GOODS`), un nom, un nom en anglais (affiché aux personnes qui ont choisi l'anglais) et une description. Une catégorie n'est jamais supprimée : désactivez-la.
- **La catégorie « Autre » (`OTHER`) doit rester active**. Une tentative de désactivation est refusée avec « La catégorie « Autre » doit rester active. ».
- Une catégorie inactive ne peut plus être choisie, ni par les demandeurs à la création, ni par le support lors d'un changement de catégorie. Les tickets qui l'utilisent déjà la conservent.

## Calendrier SLA et jours fériés

Depuis le 29 octobre 2026 (DEC-CS10-16), les délais SLA sont comptés **24 h/24 et 7 j/7** (voir [SLA et calendrier](sla.md)). L'écran <span class="ui">Calendrier SLA</span> le rappelle en haut de page : « Les délais SLA sont comptés 24 h/24, 7 j/7 : les week-ends et les jours fériés comptent. Cette liste n'a donc aucun effet sur les délais ; elle sert seulement si le mode « heures ouvrées » est réactivé (SLA_MODE=business-hours). »

La liste des jours fériés reste gérable ici. Elle ne compte que si l'équipe technique réactive le mode « heures ouvrées » : l'écran rappelle alors que les délais sont comptés du lundi au vendredi, de 8 h à 18 h (heure de Lomé), hors jours fériés officiels listés. Les horaires et les jours ouvrés de ce mode sont fixes ; seuls les jours fériés se gèrent ici.

Pour ajouter un jour férié (administrateurs) : renseignez la <span class="ui">Date</span> et le <span class="ui">Libellé</span> (par exemple le nom de la fête), puis cliquez sur <span class="ui">Ajouter</span>. Saisir une date déjà présente met à jour son libellé. Pour retirer un jour, cliquez sur <span class="ui">Supprimer</span> à côté de lui.

!!! warning "Saisir le calendrier officiel à l'avance (mode heures ouvrées)"
    La liste n'est pas pré-remplie. Saisissez les jours fériés officiels de l'année dès qu'ils sont connus. Les échéances SLA sont calculées au moment de la création du ticket, d'un changement de priorité ou d'une réouverture : un jour férié ajouté après coup ne déplace pas les échéances déjà calculées, il ne s'applique qu'aux calculs suivants.

Si la liste est vide, l'écran affiche « Aucun jour férié enregistré » et « Ajoutez le calendrier officiel pour des SLA exacts. ».

## Statuts et priorités

![Écran « Statuts et priorités » (administrateur)](images/admin-workflow.jpg)

*Repères de la capture :*

1. <span class="ui">Statuts</span> : libellés français et anglais, signification, ordre, délai en pause.
2. <span class="ui">Ajouter un statut</span>.
3. Les passages possibles : ligne = statut de départ, colonne = statut d'arrivée.
4. <span class="ui">Enregistrer les statuts</span> (les priorités se règlent plus bas, de la même façon).

L'écran <span class="ui">Statuts et priorités</span> (administrateurs ; les responsables le consultent en lecture seule) règle le vocabulaire des tickets (DEC-CS11-02).

- **Statuts** : renommez un statut en français et en anglais, changez l'ordre d'affichage, cochez <span class="ui">Délai en pause</span> pour les statuts d'attente. <span class="ui">Ajouter un statut</span> crée un statut à vous, de signification <span class="ui">En cours de traitement</span> ou <span class="ui">En attente (tiers)</span> — par exemple `WAITING_SELLER` « En attente du vendeur ». Les statuts système ne peuvent être ni retirés ni désactivés.
- **Passages possibles** : la grille indique, pour chaque statut de départ (ligne), vers quels statuts un agent peut passer le ticket à la main (colonne). Les nouveaux statuts apparaissent ensuite comme boutons dans le panneau <span class="ui">Actions sur le ticket</span>.
- **Priorités** : libellés FR/EN, rang (plus grand = plus urgent), <span class="ui">Choisie par le demandeur</span> (jamais pour la priorité critique) et la priorité <span class="ui">Par défaut</span>. Une nouvelle priorité reçoit des objectifs SLA par défaut, à ajuster dans <span class="ui">Calendrier SLA</span>.

Les règles du cycle de vie sont vérifiées à l'enregistrement : un statut encore utilisé par un ticket ne peut pas être retiré, et un réglage qui casserait le cycle (plus de statut fermé, par exemple) est refusé avec un message.

## Droits par rôle

![Écran « Droits par rôle » (administrateur)](images/admin-permissions.jpg)

*Repères de la capture :*

1. Le rôle à régler.
2. Les colonnes <span class="ui">Le sien</span>, <span class="ui">Attribué</span>, <span class="ui">Libre</span> et <span class="ui">Tous</span>.
3. Une action, avec une case par cas.
4. <span class="ui">Enregistrer les droits</span>.
5. <span class="ui">Revenir aux droits d'origine</span>.

L'écran <span class="ui">Droits</span> (administrateurs) montre, rôle par rôle, qui peut faire quoi sur un ticket : <span class="ui">Le sien</span> (ticket créé par la personne), <span class="ui">Attribué</span>, <span class="ui">Libre</span> (sans agent) ou <span class="ui">Tous</span>. Cochez ou décochez, puis <span class="ui">Enregistrer les droits</span> ; <span class="ui">Revenir aux droits d'origine</span> remet la matrice de départ. Les règles de sécurité sont vérifiées : un demandeur ne peut jamais voir le ticket d'un autre ni les notes internes.

## Réponses types et textes des notifications

![Écran « Réponses et messages »](images/admin-templates.jpg)

*Repères de la capture :*

1. <span class="ui">Réponses types</span> : les textes que les agents insèrent.
2. <span class="ui">Ajouter une réponse type</span>.
3. <span class="ui">Notifications envoyées aux utilisateurs</span> : le texte de chaque notification, en français et en anglais.

L'écran <span class="ui">Réponses et messages</span> a deux parties :

- **Réponses types** (responsables et administrateurs) : <span class="ui">Ajouter une réponse type</span>, avec un titre, une langue, une catégorie (ou <span class="ui">Toutes</span>) et le texte. Les agents les insèrent dans leurs réponses avec la liste <span class="ui">Réponses types</span> du ticket.
- **Notifications envoyées aux utilisateurs** (administrateurs) : le titre et le message de chaque notification, en français et en anglais. Chacun reçoit la notification dans la langue choisie dans l'application. Repères utilisables : `{ticket}`, `{subject}`, `{status}`, `{decision}`.
