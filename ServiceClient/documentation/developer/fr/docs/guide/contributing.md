# Contribuer

Cette page condense `CONTRIBUTING.md`, la [development matrix](/developer/en/project/DEVELOPMENT-MATRIX/) et les
documents de gouvernance en une liste de travail. La règle qui sous-tend le tout : un travail approuvé n'est jamais
modifié en silence, et chaque changement laisse le dépôt reproductible.

## Avant de commencer

Lisez, dans cet ordre : `README.md`, [Project continuity](/developer/en/project/PROJECT-CONTINUITY/),
[Development matrix](/developer/en/project/DEVELOPMENT-MATRIX/), [References](/developer/en/governance/REFERENCES/) et
le README de la partie que vous allez modifier (`backend/README.md`, `frontend/README.md`, `frontend/I18N.md`). Ne
créez pas de nouveau plan si un plan existant couvre déjà la tâche.

## Démarche de développement

1. Choisissez un élément existant de la matrice et confirmez son statut et ses dépendances.
2. Réalisez uniquement le périmètre approuvé.
3. Ajoutez ou mettez à jour les tests.
4. Mettez à jour la documentation lorsque le comportement ou l'architecture change.
5. Mettez à jour la matrice de développement, et le point de continuité lorsque le travail en cours change.
6. Committez avec un Conventional Commit.
7. Ouvrez une pull request vers `main`.

`CONTRIBUTING.md` n'impose pas de convention de nommage des branches. Travaillez sur une branche dédiée, jamais
directement sur `main` : les workflows de CI s'exécutent sur les pull requests et les pushs sur `main`, et la
documentation est déployée depuis `main`.

## Messages de commit

Utilisez les [Conventional Commits](https://www.conventionalcommits.org) avec une portée :

```text
feat(scope): add ticket assignment
fix(scope): reject invalid ticket transition
docs(scope): document ticket lifecycle
test(scope): add ticket lifecycle tests
refactor(scope): reorganize support application layer
chore(scope): update CI configuration
```

## Description d'une pull request

GitHub remplit la description à partir de `.github/PULL_REQUEST_TEMPLATE.md` (résumé et liste de la définition de
« terminé ») ; les relecteurs sont demandés d'après `.github/CODEOWNERS` (`@OWNER` provisoire tant que Maplenou n'a
pas désigné son équipe). Les anomalies et demandes de changement s'ouvrent avec les formulaires de
`.github/ISSUE_TEMPLATE/`. Une pull request explique :

- ce qui a changé et pourquoi ;
- quel élément de la matrice elle réalise ;
- les tests exécutés ;
- la documentation mise à jour ;
- tout impact sur l'architecture ou les règles métier.

## Définition de « terminé »

Un changement est terminé lorsque toutes les cases applicables sont cochées :

- [ ] Tests ajoutés ou mis à jour, et suites au vert : `./scripts/test.sh` (ou la suite concernée ; voir [Tests](testing.md)).
- [ ] Les textes visibles existent en **français et en anglais** (sinon `defineMessages` fait échouer `npm run typecheck`).
- [ ] Les nouveaux codes d'erreur d'API ont un texte anglais dans `frontend/src/lib/i18n/messages/errors.ts`.
- [ ] Documentation mise à jour **dans les deux langues** avec les mêmes noms de fichiers : guides utilisateur et
      personnel pour un comportement visible, guide du développeur (`docs/guide/` et
      `documentation/developer/fr/docs/guide/`) pour un changement de code.
- [ ] Référentiel du projet mis à jour si nécessaire (en anglais uniquement) : traçabilité des exigences,
      architecture, modèle de données, spécification d'API, `openapi.json` régénéré, diagrammes UML/Mermaid.
- [ ] `python documentation/build.py --strict` passe (au moins pour les sites modifiés).
- [ ] Matrice de développement, point de continuité et `CHANGELOG.md` (`[Unreleased]`, Keep a Changelog) mis à jour.
- [ ] Aucun secret, fichier `.env`, résultat de build ou fichier généré committé.
- [ ] La configuration de la pile Docker vient de `.env` : un nouveau paramètre est une variable documentée dans
      `.env.example` et utilisée sous la forme `${VAR:?…}` dans les fichiers Compose — jamais une valeur écrite en dur
      dans `docker-compose*.yml`.
- [ ] Chaque nouveau cache est borné (taille maximale et durée de vie), invalidé après le commit et ne mélange jamais
      les données de plusieurs utilisateurs — voir [Cache et espace disque](caching.md).
- [ ] Les images Docker restent petites : build multi-étapes, image de base légère et fixée (réutilisez une image
      déjà employée par le projet), cache mounts BuildKit, `.dockerignore` à jour ; vérifiez la taille avec
      `docker images` et `docker system df`.

Au niveau de la matrice, un élément n'est `DONE` que lorsque la réalisation, la validation, les diagrammes, la CI, les
mises à jour de continuité **et l'approbation du responsable du projet** sont réunis. Vocabulaire des statuts :
`PLANNED`, `IN_PROGRESS`, `BLOCKED`, `DONE`, `DEFERRED`, `CANCELLED`.

## Maîtrise des changements

- **Les nouvelles idées** (produit ou architecture) sont proposées avant toute réalisation : consignez-les dans
  l'[Upgrade plan](/developer/en/governance/UPGRADE-PLAN/) (problème, bénéfice, périmètre, dépendances, risques,
  impact sur les tests et la documentation, effort, impact sur l'échéance). Une amélioration n'entre dans la matrice
  qu'après approbation explicite.
- **Les éléments approuvés** (CS-01, CS-02 et les décisions approuvées DEC-001 … DEC-007) ne sont pas modifiés en
  silence ; un changement nécessaire devient une demande de changement.
- **Les décisions en attente d'approbation** sont listées dans les
  [architecture decisions](/developer/en/architecture/decisions/) et le point de continuité ; ne construisez pas sur
  une interprétation non confirmée par le responsable du projet sans le signaler.
- **Les changements de base de données** sont de nouvelles migrations Flyway ; une migration appliquée quelque part
  n'est jamais modifiée.
- **Les versions** suivent `RELEASE.md` : Semantic Versioning, changelog, CI au vert, revue de sécurité des
  changements sensibles, étiquette Git. Une version ne décrit que ce qui a été réalisé.

## Signalements de sécurité et mise en forme

Les problèmes de sécurité se signalent en privé, comme le décrit `SECURITY.md` — jamais dans une issue publique.
`.editorconfig` fixe la mise en forme pour tous les éditeurs (UTF-8, fins de ligne LF — CRLF pour les scripts PowerShell et batch —, retour à la ligne final,
2 espaces ; 4 pour Java, Python et PowerShell). `LICENSE` est une licence propriétaire provisoire que Maplenou doit confirmer.

## Passation

Avant de transmettre le travail (à une personne ou à un agent) : arbre de travail propre, matrice et point de
continuité à jour, blocages et travail inachevé documentés, décisions importantes consignées, et prochain élément
sans ambiguïté — comme l'exige le [Reproduction guide](/developer/en/governance/REPRODUCTION/).
