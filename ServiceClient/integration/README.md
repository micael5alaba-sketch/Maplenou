# Intégration avec le reste du dépôt Maplenou — *Integration with the rest of the Maplenou repository*

Ce dossier garde, **sous forme de correctifs (patches)**, les modifications que le service client et le site public
demandent **en dehors** de `ServiceClient/` et `Site_vitrine/`. Elles ne sont pas appliquées dans la branche : le
propriétaire du dépôt décide s'il les intègre, quand, et les relit avant.

*This folder keeps, as patches, the changes that the customer service and the public site need **outside**
`ServiceClient/` and `Site_vitrine/`. They are not applied in the branch: the repository owner decides whether and
when to apply them, after review.*

| Correctif / *Patch* | Fichiers touchés / *Files* | Pourquoi / *Why* |
| --- | --- | --- |
| `bakend/supportbridge.patch` | `Bakend/` : paquet `supportbridge`, `SecurityConfig`, `AuditAction`, `SupportTicketService`, `application.properties`, `docker-compose.yml`, tests, `CLAUDE.md`, `MANUEL.md`, `mvnw` exécutable | API interne lecture seule `/internal/v1/**` + 4 actions, jeton `SUPPORT_SERVICE_TOKEN`, ancien module support en lecture seule (`SUPPORT_LEGACY_READ_ONLY`). *Read-only internal API + 4 actions used by ServiceClient.* |
| `frontend/support-push-notification.patch` | `frontend/lib/services/push_notification_service.dart` | Une notification du service client ouvre le ticket dans le navigateur (liens `http(s)` seulement). *A support notification opens the ticket in the browser.* |
| `root/readme-demarrage.patch` | `README.md`, `DEMARRAGE.md` (racine) | Présente `ServiceClient/` et `Site_vitrine/` et leur démarrage. *Lists the two folders and how to start them.* |

## Sans ces correctifs — *Without these patches*

- `ServiceClient` fonctionne en mode démo (`ECOSYSTEM_MODE=demo`) et la connexion par compte Maplenou
  (`/api/auth/login` de Bakend, déjà présent) fonctionne. Seules la lecture des commandes/paiements/livraisons et
  les 4 actions demandent `supportbridge`. *Demo mode and Maplenou sign-in work; order lookups and actions need it.*
- `Site_vitrine` n'utilise que des points publics déjà présents dans Bakend (`/api/categories`, `/api/products`,
  `/api/shops`, `/api/delivery-pricing-tiers`, `/api/newsletter/subscribe`) : **aucun correctif nécessaire**.

## Appliquer (propriétaire du dépôt) — *Apply (repository owner)*

```bash
git switch -c feat/supportbridge origin/main
git apply --check ServiceClient/integration/bakend/supportbridge.patch   # vérifie sans rien changer
git apply ServiceClient/integration/bakend/supportbridge.patch
cd Bakend && ./mvnw test                                               # base PostgreSQL + Redis, comme backend-ci.yml
```

Vérifié le 2026-11-08 : les trois correctifs s'appliquent sans conflit sur `main` (`da76cf2`, commission intégrée au
prix) et Bakend compile avec. *Checked on 2026-11-08: the three patches apply cleanly on `main` (`da76cf2`) and
Bakend compiles.*
