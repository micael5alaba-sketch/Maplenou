# Information Architecture & Navigation

## Requester

```text
Service Client
├── Accueil
├── Aide & Support
│   ├── Mes tickets
│   ├── Créer un ticket
│   └── Ticket
│       ├── Vue d'ensemble
│       ├── Conversation
│       ├── Pièces jointes
│       └── Historique visible
└── Profil / Compte
```

## Support

```text
Maplenou Support
├── Tableau de bord
├── Tickets
│   ├── File d'attente
│   ├── Mes tickets
│   └── Ticket
│       ├── Vue d'ensemble
│       ├── Conversation
│       ├── Notes internes
│       ├── Pièces jointes
│       ├── Contexte autorisé
│       ├── SLA
│       └── Historique / Audit
├── Clients
├── Rapports
└── Paramètres
```

Support sections remain role-governed.

Primary requester path: `Aide & Support → Mes tickets → Ticket`.

Creation path: `Aide & Support → Créer un ticket`.

Primary support path: `Tableau de bord → Tickets → File d'attente → Ticket`.

UI navigation does not replace backend authorization.
