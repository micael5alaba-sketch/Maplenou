# Escalade

Escalader un ticket, c'est le signaler aux responsables support pour qu'ils interviennent (BR-064). L'escalade est un **marqueur** posé sur le ticket : ce n'est pas un statut. Le ticket garde son statut, sa priorité, son agent attribué et son SLA.

## Quand escalader

Les règles approuvées ne fixent pas de liste de cas d'escalade : c'est une décision de l'agent. Elle est utile quand la suite du traitement dépasse votre autorité ou demande l'arbitrage d'un responsable, par exemple :

- le ticket doit être réattribué à un autre agent (seul un responsable peut réattribuer, BR-058, BR-059) ;
- vous ne pourrez pas tenir le délai SLA et le ticket a besoin d'être pris en main ou redistribué ;
- le cas est sensible ou inhabituel et demande une décision que vous ne pouvez pas prendre seul.

Ces exemples sont des repères, pas des règles. En cas de doute, parlez-en à votre responsable.

## Qui peut escalader

| Rôle | Peut escalader |
| --- | --- |
| Agent support | Les tickets qui lui sont attribués et les tickets non attribués |
| Responsable support | Tous les tickets |
| Administrateur | Tous les tickets |

Un ticket ne peut être escaladé qu'une fois, et pas lorsqu'il est <span class="ui">Fermé</span>. Un agent ne peut pas escalader le ticket d'un collègue (ticket en lecture seule pour lui).

## Comment escalader

1. Ouvrez le ticket.
2. Dans <span class="ui">Actions sur le ticket</span>, cliquez sur <span class="ui">Escalader au responsable</span>.
3. Dans la fenêtre <span class="ui">Escalader ce ticket ?</span>, remplissez le <span class="ui">Motif de l'escalade</span> (obligatoire, 500 caractères maximum) : ce qui bloque, ce qui a déjà été fait, ce que vous attendez du responsable.
4. Cliquez sur <span class="ui">Escalader</span>. Le message « Ticket escaladé. » confirme.

!!! tip "Un motif utile"
    Le responsable doit comprendre la situation sans relire tout le ticket. Pour les détails, ajoutez une note interne avant d'escalader.

## Ce qui se passe après l'escalade

- Le ticket porte le badge <span class="ui">Escaladé</span> dans son en-tête et dans la file d'attente.
- Les **responsables support actifs** reçoivent la notification « Ticket escaladé » (sauf celui qui a escaladé lui-même).
- L'historique enregistre <span class="ui">Ticket escaladé</span> avec votre motif.
- Les responsables et administrateurs voient la date dans le panneau des actions (« Escaladé le … »).
- Le ticket est compté dans la note « N escaladés » de la tuile <span class="ui">SLA dépassé</span> du tableau de bord tant qu'il est ouvert, et retrouvé avec la case <span class="ui">Escaladés</span> de la file.

L'escalade ne change rien d'autre : l'agent attribué le reste, le SLA continue de courir. Le demandeur n'est pas informé de l'escalade et ne la voit pas dans son historique.

## Ce que fait le responsable

Le responsable dispose de tous les droits opérationnels sur le ticket : il peut le réattribuer avec <span class="ui">Attribuer à un agent</span>, se l'attribuer, changer la priorité, répondre, ajouter une note interne pour expliquer sa décision, résoudre ou clôturer.

!!! note "Pas d'annulation d'escalade"
    L'application ne propose pas de retirer le marqueur <span class="ui">Escaladé</span> : il reste sur le ticket, y compris après résolution, comme trace de l'escalade.
