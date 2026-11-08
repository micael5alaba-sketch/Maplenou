# Réouverture

Quand le problème réapparaît après la résolution, le demandeur peut demander la réouverture de son ticket. C'est le support qui décide : le demandeur ne peut jamais rouvrir lui-même un ticket (BR-049, BR-050).

## Règles de réouverture

| Règle | Référence |
| --- | --- |
| Seul le demandeur peut demander la réouverture, et seulement pour **son propre** ticket <span class="ui">Résolu</span>. | BR-048, DEC-007 |
| Un ticket <span class="ui">Fermé</span> ne peut pas être rouvert : le demandeur doit créer un nouveau ticket. | BR-052, BR-053 |
| Une seule demande peut être en attente à la fois sur un ticket. | Fonctionnement de l'application |
| Un agent support ou un responsable autorisé examine et décide. | BR-050 |
| Une réouverture acceptée ramène le ticket <span class="ui">En cours</span>. | BR-051 |
| Les demandes et les décisions sont enregistrées dans l'historique. | BR-054 |
| L'historique SLA d'origine est conservé et une nouvelle période de résolution commence. | BR-055 |

## Qui peut décider

- l'**agent attribué** au ticket ;
- tout **responsable support** ;
- tout **administrateur**.

Un agent qui n'est pas attribué au ticket voit la demande dans l'historique, mais pas les boutons de décision.

## Être averti d'une demande

Lorsqu'un demandeur demande la réouverture :

- l'agent attribué reçoit la notification « Demande de réouverture » ; si le ticket n'est pas attribué, ce sont les responsables support actifs ;
- l'historique enregistre <span class="ui">Réouverture demandée</span> avec le motif du demandeur ;
- le ticket reste <span class="ui">Résolu</span> tant que la demande n'est pas traitée.

## Accepter ou refuser une demande

En haut de l'écran du ticket, l'encadré « Demande de réouverture du *date* » affiche le motif du demandeur entre guillemets.

1. Lisez le motif, la conversation et l'historique.
2. Rédigez la <span class="ui">Note de décision (visible par le demandeur)</span> : expliquez votre décision en quelques phrases claires. Elle est facultative, mais recommandée, surtout en cas de refus.
3. Cliquez sur :
    - <span class="ui">Rouvrir (En cours)</span> pour accepter : le ticket repasse <span class="ui">En cours</span> (message « Ticket rouvert. ») ;
    - <span class="ui">Refuser</span> pour refuser : le ticket reste <span class="ui">Résolu</span> (message « Demande refusée. »).

!!! warning "La note de décision est visible"
    Contrairement aux notes internes, la note de décision est montrée au demandeur dans l'historique de son ticket. N'y mettez aucune information interne.

## Après une réouverture acceptée

- Le ticket est <span class="ui">En cours</span>, avec le même agent attribué.
- Une **nouvelle période de résolution** SLA démarre au moment de l'acceptation, selon la priorité actuelle ; l'échéance de première réponse ne change pas (voir [SLA et calendrier](sla.md)).
- L'historique enregistre <span class="ui">Réouverture acceptée</span> puis <span class="ui">Statut modifié</span>.
- Le demandeur est notifié que sa demande a été acceptée.

Reprenez ensuite le traitement normalement : répondez au demandeur, puis marquez à nouveau le ticket résolu une fois le problème réglé.

## Après un refus

- Le ticket reste <span class="ui">Résolu</span>.
- L'historique enregistre <span class="ui">Réouverture refusée</span> avec votre note.
- Le demandeur est notifié que sa demande a été refusée. Il peut faire une nouvelle demande tant que le ticket est <span class="ui">Résolu</span>.

## Clôturer un ticket qui a une demande en attente

Tant qu'une demande de réouverture est en attente, le ticket ne peut pas être clôturé : le bouton <span class="ui">Clôturer le ticket</span> n'est pas proposé, et une tentative renvoie « Une demande de réouverture est en attente : traitez-la avant de fermer le ticket. ». Décidez d'abord de la demande.

## Demande concernant un ticket fermé

Si un demandeur vous contacte au sujet d'un ticket <span class="ui">Fermé</span>, expliquez-lui qu'il doit créer un nouveau ticket depuis son espace, en citant si possible le numéro de l'ancien ticket dans sa description.
