# Questions fréquentes

Les questions que l'équipe support se pose le plus souvent. Cliquez sur une question pour afficher la réponse.

## Tickets et droits

??? question "Pourquoi je ne peux pas répondre à ce ticket ?"
    Trois cas possibles :

    - le ticket est **attribué à un collègue** : l'encadré <span class="ui">Lecture seule</span> s'affiche, seul l'agent attribué ou un responsable peut le modifier ;
    - le ticket est **non attribué** : cliquez d'abord sur <span class="ui">Prendre en charge</span>, puis répondez ;
    - le ticket est **fermé** : aucune action n'est possible (« Ticket fermé : aucune action possible. »).

    Voir [Rôles et accès](roles-and-access.md).

??? question "Un collègue est absent : puis-je reprendre son ticket ?"
    Non, un agent ne peut pas réattribuer le ticket d'un autre agent (BR-058). Demandez à un responsable support : il peut vous l'attribuer avec <span class="ui">Attribuer à un agent</span>. Si le ticket est bloqué, vous pouvez aussi demander à l'agent attribué ou à un responsable de l'escalader.

??? question "Une note interne peut-elle être vue par le demandeur ?"
    Non. Les notes internes et les fichiers joints à une note interne ne sont jamais visibles par le demandeur (BR-016). En revanche, tout ce que vous écrivez dans l'onglet <span class="ui">Conversation</span> et la note de décision d'une réouverture sont visibles par lui.

??? question "Puis-je modifier ou supprimer un message déjà envoyé ?"
    Non. L'application ne permet pas de modifier ou de supprimer un message ou une note interne, et l'historique ne peut pas être modifié (BR-018). Si vous avez fait une erreur, envoyez un nouveau message qui la corrige.

## Priorité et SLA

??? question "Le demandeur indique que c'est urgent : dois-je passer le ticket en Critique ?"
    Pas automatiquement. Le badge <span class="ui">Urgence signalée</span> est une information du demandeur ; il ne fixe jamais la priorité <span class="ui">Critique</span> (BR-037). Évaluez la situation et choisissez la priorité adaptée. Voir [Bonnes pratiques](good-practices.md).

??? question "Le SLA s'arrête-t-il quand le ticket est En attente du client ?"
    Non. Le délai continue de courir pendant <span class="ui">En attente du client</span> : la suspension n'a pas été approuvée. Demandez toutes les informations nécessaires en une fois et relancez si besoin. Voir [SLA et calendrier](sla.md).

??? question "Pourquoi le ticket est passé en « Délai dépassé » juste après un changement de priorité ?"
    Le changement de priorité recalcule les échéances depuis la création du ticket (première réponse, si personne n'a encore répondu) et depuis le début de la période de résolution. Si le nouveau délai, plus court, est déjà écoulé, l'état devient immédiatement <span class="ui">Délai dépassé</span>. L'historique montre l'entrée <span class="ui">SLA recalculé</span>.

## Résolution, réouverture, escalade

??? question "Je ne trouve pas le bouton Clôturer le ticket sur un ticket résolu."
    Une demande de réouverture est probablement en attente : l'encadré « Demande de réouverture » apparaît en haut du ticket. Acceptez-la ou refusez-la d'abord ; la clôture redevient possible si vous refusez. Voir [Réouverture](reopening.md).

??? question "Le demandeur veut rouvrir un ticket fermé."
    Ce n'est pas possible (BR-052). Invitez-le à créer un nouveau ticket depuis son espace, en citant le numéro de l'ancien.

??? question "Comment annuler une escalade ?"
    L'application ne permet pas de retirer le marqueur <span class="ui">Escaladé</span>. Si l'escalade n'est plus utile, indiquez-le dans une note interne et prévenez votre responsable. Voir [Escalade](escalation.md).

## Écran et incidents

??? question "Le panneau Commande affiche « Service momentanément indisponible. »"
    Le service Maplenou qui fournit ces informations ne répond pas pour le moment. Le reste du ticket fonctionne normalement : continuez à traiter le ticket et rechargez la page plus tard pour afficher le contexte.

??? question "Le message « Le ticket a été modifié entre-temps. Rechargez puis réessayez. » s'affiche."
    Un collègue a modifié le même ticket en même temps que vous. Rechargez la page, vérifiez ce qui a changé (onglet <span class="ui">Historique</span>) et refaites votre action si elle est toujours nécessaire.
