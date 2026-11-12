# Bonnes pratiques

Ces recommandations aident à offrir un service homogène et sûr. Elles complètent les règles de l'application sans les remplacer : quand une règle s'applique (confidentialité des notes internes, priorité Critique, pièces jointes), elle est rappelée avec sa référence.

## Rédiger une réponse au demandeur

- **Saluez et nommez le problème** : reformulez en une phrase ce que vous avez compris de la demande.
- **Soyez clair et concret** : phrases courtes, une idée par paragraphe, étapes numérotées quand le demandeur doit agir.
- **Dites ce qui va se passer** : ce que vous avez fait, ce que vous allez faire, ce que vous attendez de lui.
- **Restez courtois et neutre**, même face à un demandeur mécontent. Pas de jargon interne (statuts techniques, codes d'erreur, noms d'outils).
- **Relisez avant d'envoyer** : un message envoyé dans la conversation ne peut pas être modifié et reste dans l'historique.

## Choisir la langue de la réponse

L'application est disponible en français et en anglais. Répondez dans la langue utilisée par le demandeur dans son ticket ; s'il écrit en anglais, répondez en anglais. Évitez de mélanger les deux langues dans un même message.

Les notifications dans l'application s'affichent dans la langue choisie par l'utilisateur ; les notifications push (navigateur et applications mobiles) sont pour l'instant envoyées en français.

## Utiliser les notes internes

Les notes internes (onglet <span class="ui">Notes internes</span>) ne sont **jamais** visibles par le demandeur (BR-016). Utilisez-les pour :

- consigner une vérification (« Paiement vérifié : statut confirmé côté prestataire ») ;
- préparer une passation à un collègue ou à un responsable ;
- demander un avis à l'équipe sans exposer la discussion au demandeur ;
- expliquer une décision (changement de priorité, refus de réouverture) pour qui reprendra le ticket.

Avant d'envoyer, vérifiez toujours l'onglet actif : <span class="ui">Conversation</span> = visible par le demandeur, <span class="ui">Notes internes</span> = support seulement.

## Demander des informations au demandeur

1. Envoyez d'abord un message dans la <span class="ui">Conversation</span> qui liste **toutes** les informations nécessaires en une fois (numéro de commande, photo du colis, capture d'écran…).
2. Cliquez ensuite sur <span class="ui">Demander des informations</span> : le ticket passe <span class="ui">En attente du client</span> et le demandeur est notifié.
3. Dès qu'il répond, le ticket repasse automatiquement <span class="ui">En cours</span> et vous êtes notifié.

Le délai SLA continue de courir pendant l'attente (voir [SLA et calendrier](sla.md)) : sans réponse, relancez le demandeur par un nouveau message.

## Protéger les données personnelles et financières

!!! danger "Ne jamais demander"
    Ne demandez **jamais** un mot de passe, un code de vérification ou un numéro de carte bancaire complet, ni dans la conversation ni par un autre canal. Le support n'en a jamais besoin. Si un demandeur en envoie un, ne le recopiez pas et invitez-le à changer son mot de passe ou à contacter sa banque.

- Les informations de paiement, de reversement, de commission et de solde vendeur affichées dans le contexte du ticket sont réservées au support : ne les recopiez pas dans la conversation, sauf ce qui concerne directement le demandeur et dont il a besoin.
- Ne mentionnez pas dans un ticket les données d'un autre demandeur.
- Pour les pièces jointes, demandez seulement ce qui est utile. Formats acceptés : JPG, JPEG, PNG, WEBP, PDF ; 10 Mo par fichier, 5 fichiers par message, 50 Mo par ticket (DEC-003).

## Utiliser la priorité Critique avec discernement

La priorité <span class="ui">Critique</span> impose le délai le plus court (1 heure pour la première réponse, 4 heures pour la résolution, comptées 24 h/24) et chaque passage en Critique est audité (BR-038).

- Seul le support peut la fixer (BR-036) : le badge <span class="ui">Urgence signalée</span> d'un demandeur est un signal à évaluer, pas une obligation (BR-037).
- Les règles approuvées ne définissent pas de liste de cas « critiques ». Réservez-la aux situations qui ne peuvent pas attendre le délai d'une priorité <span class="ui">Élevée</span>, et en cas de doute, demandez l'avis de votre responsable.
- Renseignez toujours le <span class="ui">Motif</span> dans la fenêtre de confirmation (par exemple « fraude présumée ») : il est conservé dans l'historique et aide vos collègues.
- Passer en Critique un ticket déjà ancien peut le rendre immédiatement en retard : répondez aussitôt.

## Résoudre et clôturer proprement

- Avant <span class="ui">Marquer résolu</span>, envoyez un message qui résume la solution et indique au demandeur qu'il peut demander une réouverture si le problème persiste.
- Ne clôturez un ticket résolu que lorsque le demandeur n'en a plus besoin : après la clôture, il devra créer un nouveau ticket (BR-053).
- Utilisez le <span class="ui">Commentaire (historique, optionnel)</span> des fenêtres de confirmation pour garder une trace de la raison du changement.
