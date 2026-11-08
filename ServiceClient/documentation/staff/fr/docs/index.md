# Guide interne du support

Ce guide explique comment traiter les demandes d'assistance des clients, vendeurs et livreurs Maplenou dans l'espace support de l'application de service client : file d'attente, traitement d'un ticket, délais SLA, escalade, réouverture, administration et rapports.

!!! warning "Site interne"
    Ce site est réservé au personnel du support Maplenou. Il décrit des règles internes, des informations visibles uniquement par le support (notes internes, SLA, contexte de paiement ou de livraison) et les droits de chaque rôle. Ne le partagez pas avec les demandeurs : ils disposent de leur propre guide utilisateur.

## À qui s'adresse ce guide

| Public | Rôle dans l'application | Ce qui vous concerne en priorité |
| --- | --- | --- |
| Agents support | <span class="ui">Agent support</span> | [Travail quotidien](daily-work.md), [Traiter un ticket](handle-ticket.md), [Statuts et transitions](statuses-and-transitions.md), [SLA et calendrier](sla.md), [Bonnes pratiques](good-practices.md) |
| Responsables support | <span class="ui">Responsable support</span> | En plus : [Escalade](escalation.md), [Réouverture](reopening.md), réattribution ([Rôles et accès](roles-and-access.md)), [Rapports](reports.md) |
| Administrateurs | <span class="ui">Administrateur</span> | En plus : [Administration](administration.md) (activation des agents, rôles support, import des anciens tickets, catégories, calendrier SLA) |

Les demandeurs (clients, vendeurs, livreurs) créent les tickets depuis leur espace ; ils ne voient jamais ce guide ni les informations réservées au support.

## Contenu du guide

| Page | Ce que vous y trouvez |
| --- | --- |
| [Rôles et accès](roles-and-access.md) | Les trois rôles support, les demandeurs, le tableau des droits (qui peut voir, répondre, attribuer, passer en Critique, escalader, clôturer, administrer). |
| [Travail quotidien](daily-work.md) | Le tableau de bord, la file d'attente (recherche, filtres, tri), <span class="ui">Mes tickets</span>, les notifications et une routine quotidienne conseillée. |
| [Traiter un ticket](handle-ticket.md) | L'écran du ticket pas à pas : contexte commande/livraison, conversation, notes internes, pièces jointes, attribution, priorité, catégorie, statut, historique. |
| [Statuts et transitions](statuses-and-transitions.md) | Les six statuts, les changements autorisés, les changements automatiques et qui peut les faire. |
| [SLA et calendrier](sla.md) | Délais par priorité, calendrier 24 h/24 et 7 j/7, états SLA, recalculs. |
| [Escalade](escalation.md) | Quand et comment escalader un ticket au responsable. |
| [Réouverture](reopening.md) | Traiter une demande de réouverture d'un ticket résolu. |
| [Administration](administration.md) | Agents, rôles support, anciens tickets Maplenou, catégories, calendrier SLA. |
| [Rapports](reports.md) | Le rapport des tickets et la lecture des indicateurs. |
| [Bonnes pratiques](good-practices.md) | Rédaction des réponses, notes internes, confidentialité, usage de la priorité Critique. |
| [Questions fréquentes](faq.md) | Les questions courantes de l'équipe support. |

## Trouver une réponse rapidement

- **Recherche** : appuyez sur la touche <kbd>/</kbd> (ou cliquez sur la loupe en haut de page) et tapez quelques mots, par exemple « réouverture » ou « Critique ». Les résultats pointent vers la section exacte.
- **Assistant** : cliquez sur le bouton <span class="ui">Poser une question</span> en bas de page et posez votre question en langage courant (« Quels sont les délais SLA ? », « Quand escalader un ticket ? »). L'assistant répond à partir de ce guide uniquement et cite ses références : cliquez sur une référence pour ouvrir la section correspondante et vérifier.
- **Depuis l'application** : le lien <span class="ui">Guide du support</span> du menu latéral de l'espace support ouvre ce guide dans un nouvel onglet.

!!! tip "Changer de langue"
    Le sélecteur de langue en haut de page bascule entre le français et l'anglais en restant sur la même page.

## Sources des règles

Les règles décrites ici reprennent les règles métier approuvées du module (identifiants `BR-xxx`), les décisions approuvées `DEC-001` à `DEC-007` et le comportement réel de l'application. Quand une règle n'est pas encore validée par le responsable du projet, la page le signale par un encadré « en attente de validation ». En cas de doute sur une règle, demandez à votre responsable support plutôt que d'improviser.
