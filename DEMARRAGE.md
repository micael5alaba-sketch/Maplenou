# Démarrage — Maplenou

## 1. Backend (obligatoire, toujours en premier)

```powershell
cd Bakend
./start-all.ps1
```

Ce script démarre Redis si besoin, charge les secrets locaux (FedaPay, Cloudinary), puis lance
le serveur sur `http://localhost:8080`. PostgreSQL doit déjà tourner (service Windows).

Vérifier que ça a démarré : `http://localhost:8080/api/products` doit répondre `[]` ou une liste.

## 2. App mobile — téléphone physique (USB)

```powershell
adb reverse tcp:8080 tcp:8080
cd frontend
flutter run
```

Le téléphone doit être branché avec le débogage USB autorisé (popup à accepter sur l'écran).

## 3. App mobile — émulateur ou web (sans téléphone)

```powershell
cd frontend
flutter run -d chrome      # web
flutter run -d emulator-5554  # émulateur Android déjà lancé
```

## 4. Back-office Admin (Next.js, optionnel)

```powershell
cd Admin_frontend/maplenou
npm run dev
```

Accessible sur `http://localhost:3000`.

## Ordre à respecter

Backend d'abord (§1), toujours. Le reste dépend de lui.

## Comptes de test

| Rôle | Téléphone | Mot de passe |
|---|---|---|
| Admin | `+22890000000` | `Micael2005@` |

## Problème fréquent

| Symptôme | Cause |
|---|---|
| Login échoue en 403/500 | Redis pas démarré (`start-all.ps1` s'en charge normalement) |
| App mobile ne charge rien (téléphone) | `adb reverse tcp:8080 tcp:8080` pas fait, ou refait après débranchement/rebranchement du câble |
| `adb` : device offline | Débrancher/rebrancher le câble, vérifier le popup d'autorisation sur le téléphone |
| Port 8080 déjà utilisé | Une instance du backend tourne déjà — l'arrêter avant de relancer |
