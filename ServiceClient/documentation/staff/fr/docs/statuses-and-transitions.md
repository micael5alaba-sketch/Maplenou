# Statuts et transitions

Un ticket suit un cycle de vie contrôlé : seuls certains changements de statut sont autorisés (BR-010). L'écran du ticket ne propose que les boutons valides depuis le statut actuel ; toute autre demande est refusée et le statut reste inchangé.

## Les six statuts

| Statut | Signification | Ouvert ? |
| --- | --- | :---: |
| <span class="ui">Nouveau</span> | Ticket créé par le demandeur, personne ne l'a encore pris. Tout ticket commence ici (BR-009). | Oui |
| <span class="ui">Attribué</span> | Un agent est responsable du ticket mais n'a pas encore commencé à le traiter. | Oui |
| <span class="ui">En cours</span> | Le support traite activement la demande. | Oui |
| <span class="ui">En attente du client</span> | Le support attend une information ou une action du demandeur. | Oui |
| <span class="ui">Résolu</span> | Le problème est traité. Le demandeur peut encore demander une réouverture. | Non |
| <span class="ui">Fermé</span> | Ticket finalisé. Plus aucune action possible ; une nouvelle demande nécessite un nouveau ticket. | Non |

Les tickets « ouverts » sont ceux que comptent la tuile <span class="ui">Tickets ouverts</span> et le filtre <span class="ui">Ouverts (tous)</span>.

## Diagramme des transitions autorisées

```mermaid
stateDiagram-v2
    state "Nouveau" as NEW
    state "Attribué" as ASSIGNED
    state "En cours" as IN_PROGRESS
    state "En attente du client" as WAITING
    state "Résolu" as RESOLVED
    state "Fermé" as CLOSED

    [*] --> NEW : création par le demandeur
    NEW --> ASSIGNED : attribution (automatique)
    ASSIGNED --> IN_PROGRESS : Démarrer le traitement / première réponse
    ASSIGNED --> WAITING : Demander des informations
    IN_PROGRESS --> WAITING : Demander des informations
    WAITING --> IN_PROGRESS : Reprendre le traitement / réponse du demandeur
    IN_PROGRESS --> RESOLVED : Marquer résolu
    WAITING --> RESOLVED : Marquer résolu
    RESOLVED --> CLOSED : Clôturer le ticket
    RESOLVED --> IN_PROGRESS : réouverture acceptée
    CLOSED --> [*]
```

## Tableau des transitions et qui peut les faire

| De | Vers | Comment | Qui |
| --- | --- | --- | --- |
| <span class="ui">Nouveau</span> | <span class="ui">Attribué</span> | Automatique lors de la première attribution (<span class="ui">Prendre en charge</span> ou <span class="ui">Attribuer à un agent</span>). Il n'existe pas de bouton de statut pour cette étape. | Agent (sur un ticket non attribué), responsable, administrateur (attribution à un agent) |
| <span class="ui">Attribué</span> | <span class="ui">En cours</span> | <span class="ui">Démarrer le traitement</span>, ou automatiquement à la première réponse visible du support | Agent attribué, responsable, administrateur |
| <span class="ui">Attribué</span> ou <span class="ui">En cours</span> | <span class="ui">En attente du client</span> | <span class="ui">Demander des informations</span> | Agent attribué, responsable, administrateur |
| <span class="ui">En attente du client</span> | <span class="ui">En cours</span> | <span class="ui">Reprendre le traitement</span>, ou automatiquement quand le demandeur répond | Agent attribué, responsable, administrateur ; le demandeur indirectement par sa réponse |
| <span class="ui">En cours</span> ou <span class="ui">En attente du client</span> | <span class="ui">Résolu</span> | <span class="ui">Marquer résolu</span> (avec confirmation) | Agent attribué, responsable, administrateur |
| <span class="ui">Résolu</span> | <span class="ui">Fermé</span> | <span class="ui">Clôturer le ticket</span> (avec confirmation), seulement s'il n'y a pas de demande de réouverture en attente | Agent attribué, responsable, administrateur |
| <span class="ui">Résolu</span> | <span class="ui">En cours</span> | Uniquement en acceptant une demande de réouverture du demandeur (<span class="ui">Rouvrir (En cours)</span>) | Agent attribué, responsable, administrateur |

Chaque changement de statut est enregistré dans l'historique avec son auteur, sa date et, le cas échéant, votre commentaire (BR-017). Le demandeur est notifié des changements de statut de son ticket.

## Transitions interdites

- <span class="ui">Nouveau</span> → <span class="ui">Fermé</span> est explicitement interdit (BR-010) : un ticket doit être traité et résolu avant d'être fermé.
- <span class="ui">Résolu</span> → un statut actif sans demande de réouverture acceptée est interdit, y compris pour le support (BR-049). Le demandeur ne peut pas rouvrir directement.
- <span class="ui">Fermé</span> est définitif : aucun changement de statut n'est possible, ni par le support, ni par le demandeur (BR-052). Une nouvelle demande nécessite un nouveau ticket (BR-053).
- Un agent ne peut pas changer le statut d'un ticket attribué à un collègue, ni d'un ticket non attribué tant qu'il ne l'a pas pris en charge.

En cas de tentative refusée, l'application affiche « Transition impossible : … » avec les deux statuts concernés.

## Changements automatiques

| Événement | Changement |
| --- | --- |
| Première attribution d'un ticket <span class="ui">Nouveau</span> | <span class="ui">Nouveau</span> → <span class="ui">Attribué</span> |
| Première réponse visible du support sur un ticket <span class="ui">Attribué</span> | <span class="ui">Attribué</span> → <span class="ui">En cours</span> |
| Réponse du demandeur sur un ticket <span class="ui">En attente du client</span> | <span class="ui">En attente du client</span> → <span class="ui">En cours</span> |
| Réouverture acceptée | <span class="ui">Résolu</span> → <span class="ui">En cours</span> |

Une note interne ne change jamais le statut. Un ticket <span class="ui">Résolu</span> que le demandeur ne fait pas rouvrir est **fermé automatiquement après 7 jours** (DEC-CS11-04) ; l'historique indique « Fermé automatiquement ». Les administrateurs peuvent ajouter des statuts et changer les passages possibles (voir [Administration](administration.md#statuts-et-priorites)).

!!! note "Statut cible d'une réouverture"
    La règle BR-051 prévoit qu'une réouverture acceptée ramène le ticket <span class="ui">En cours</span>, sauf si le support choisit explicitement un autre statut actif valide. L'écran propose aujourd'hui uniquement <span class="ui">Rouvrir (En cours)</span>.
