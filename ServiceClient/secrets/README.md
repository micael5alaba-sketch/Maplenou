# secrets/ — keys that must never be committed

This folder is mounted **read-only** into the backend container at `/run/secrets/maplenou`
(`docker-compose.yml`). Everything here except this README is ignored by Git and by every Docker build.

| File | What it is | Used by |
| --- | --- | --- |
| `firebase-service-account.json` | Firebase service-account key (Firebase console → Project settings → Service accounts → Generate new private key) | backend, with `FIREBASE_CREDENTIALS_FILE=/run/secrets/maplenou/firebase-service-account.json` in `.env` |

Treat these files like passwords: do not send them by e-mail or chat, do not copy them into another folder of the
repository, and generate a new key (deleting the old one in the Firebase console) if one is ever exposed.
Setup guide: `docs/guide/firebase.md`.
