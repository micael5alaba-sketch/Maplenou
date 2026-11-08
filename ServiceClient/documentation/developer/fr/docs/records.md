# Référentiel du projet (anglais)

Le référentiel du projet — exigences, UX/UI, architecture, diagrammes, rapports de validation, gouvernance et suivi
du projet — est **tenu en anglais uniquement**, dans le dossier `docs/` du dépôt. Il est publié sur le site
développeur anglais. Cette page en donne l'accès section par section.

## Pourquoi le référentiel n'est pas traduit

Le référentiel sert de source unique de vérité : exigences numérotées (FR-xxx, BR-xxx, AC-xxx), décisions (DEC-xxx),
rapports de validation et matrice de développement. Une seule langue évite que deux versions divergent. Le guide du
développeur, lui, est traduit, et les interfaces et guides utilisateur et personnel existent dans les deux langues.

## Suivi du projet

| Document | Contenu |
| --- | --- |
| [Project Continuity](/developer/en/project/PROJECT-CONTINUITY/) | phase actuelle, décisions en attente, prochaine étape |
| [Development Matrix](/developer/en/project/DEVELOPMENT-MATRIX/) | lots CS-01 … CS-11, dépendances, statuts, définition de « terminé » |
| [Project Plan](/developer/en/project/PROJECT-PLAN/) | planning des lots |

## Exigences (CS-01)

| Document | Contenu |
| --- | --- |
| [Functional Requirements](/developer/en/requirements/functional-requirements/) | exigences fonctionnelles FR-xxx |
| [Business Rules](/developer/en/requirements/business-rules/) | règles métier BR-xxx |
| [Business Decisions](/developer/en/requirements/business-decisions/) | décisions métier DEC-001 … DEC-007 |
| [Acceptance Criteria](/developer/en/requirements/acceptance-criteria/) | critères d'acceptation AC-xxx |
| [Authorization Matrix](/developer/en/requirements/authorization-matrix/) | qui peut faire quoi |
| [Traceability](/developer/en/requirements/requirements-traceability/) | lien exigence → implémentation → tests |
| [Glossary](/developer/en/requirements/requirements-glossary/) | vocabulaire du projet |

## UX/UI (CS-02)

| Document | Contenu |
| --- | --- |
| [Overview](/developer/en/ux-ui/) | vue d'ensemble UX/UI |
| [User Journeys](/developer/en/ux-ui/user-journeys/) | parcours utilisateurs |
| [Information Architecture](/developer/en/ux-ui/information-architecture/) | organisation des écrans |
| [Screen Inventory](/developer/en/ux-ui/screen-inventory/) | liste des écrans A1–A4, B1–B4… |
| [Screen Behavior](/developer/en/ux-ui/screen-behavior/) | comportement des écrans |
| [Wireframes](/developer/en/ux-ui/wireframes/) | maquettes fil de fer |
| [Design System](/developer/en/ux-ui/design-system/) | jetons, composants, accessibilité |
| [Component Inventory](/developer/en/ux-ui/component-inventory/) | composants d'interface |
| [Responsive Design](/developer/en/ux-ui/responsive-design/) | adaptation mobile et bureau |
| [Mockups and screens](/developer/en/ux-ui/screens/) | canevas de maquettes Claude Design (EN/FR × clair/sombre × ordinateur/tablette/mobile), catalogue `/design`, captures de l'application et de la documentation |

Critères d'acceptation des livrables CS-02 : [CS-02.1](/developer/en/ux-ui/CS-02.1-acceptance-criteria/),
[CS-02.2](/developer/en/ux-ui/CS-02.2-acceptance-criteria/), [CS-02.3](/developer/en/ux-ui/CS-02.3-acceptance-criteria/),
[CS-02.4](/developer/en/ux-ui/CS-02.4-acceptance-criteria/), [CS-02.5](/developer/en/ux-ui/CS-02.5-acceptance-criteria/),
[CS-02.6](/developer/en/ux-ui/CS-02.6-acceptance-criteria/).

## Architecture (CS-03 et suivants)

| Document | Contenu |
| --- | --- |
| [Overview](/developer/en/architecture/) | schéma du système et paquets du backend |
| [Reference architecture](/developer/en/architecture/reference-architecture/) | plan suivi, styles, revue de conformité, réutilisation |
| [Decisions](/developer/en/architecture/decisions/) | décisions techniques DEC-CS03-xx |
| [Database Model](/developer/en/architecture/database-model/) | tables, règles imposées par la base, index |
| [API Specification](/developer/en/architecture/api-specification/) | endpoints, erreurs, recherche et filtres |
| [Ecosystem Integration](/developer/en/architecture/ecosystem-integration/) | `EcosystemGateway` et contrat HTTP proposé |
| [Notifications](/developer/en/architecture/notifications/) | événements, push, langues |
| [Security Review](/developer/en/architecture/security/) | contrôles et liste « avant la production » |
| [Complexity Review](/developer/en/architecture/complexity-review/) | complexité des algorithmes sur les chemins chauds, optimisations CS-12 |

## Diagrammes

| Document | Contenu |
| --- | --- |
| [Ticket State](/developer/en/diagrams/uml/ticket-state/) | cycle de vie du ticket |
| [Ticket Sequence](/developer/en/diagrams/uml/ticket-sequence/) | séquence de traitement |
| [Ticket Domain](/developer/en/diagrams/uml/ticket-domain-class/) | modèle de domaine |

## Validation

Rapports de validation : [CS-01](/developer/en/validation/CS-01-FINAL-VALIDATION/),
[CS-02](/developer/en/validation/CS-02-FINAL-VALIDATION/), [CS-03](/developer/en/validation/CS-03-VALIDATION/),
[CS-04](/developer/en/validation/CS-04-VALIDATION/), [CS-05](/developer/en/validation/CS-05-VALIDATION/),
[CS-06](/developer/en/validation/CS-06-VALIDATION/), [CS-07](/developer/en/validation/CS-07-VALIDATION/),
[CS-08](/developer/en/validation/CS-08-VALIDATION/), [CS-09](/developer/en/validation/CS-09-FINAL-VALIDATION/), [CS-10](/developer/en/validation/CS-10-VALIDATION/), [CS-11](/developer/en/validation/CS-11-VALIDATION/).

## Gouvernance

| Document | Contenu |
| --- | --- |
| [References](/developer/en/governance/REFERENCES/) | normes et références suivies |
| [Reproduction Guide](/developer/en/governance/REPRODUCTION/) | reproduire l'environnement et transmettre le travail |
| [Upgrade Plan](/developer/en/governance/UPGRADE-PLAN/) | améliorations proposées, soumises à approbation |
