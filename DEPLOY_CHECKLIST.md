# Deploy Checklist: METAPOWER GymTrack
**Date:** 2026-10-14 | **Deployer:** _____ | **Target:** Docker Compose (db + backend + frontend/nginx), 2 sites
**Release:** `main` @ `b21be77` + 66 uncommitted files (+5775 / −3624) — **not yet committed**

---

## 🚨 Blockers found in the working tree (fix before anything else)

- [x] **Flyway migrations restored** — `V1__init.sql` (from git) and `V2__add_gerant.sql` (recovered from `backend/target/`) are back in `src/main/resources/db/migration/`. Verified: fresh Postgres 15 → both migrations applied, `ddl-auto: validate` passes, `/actuator/health` UP.
- [x] **Frontend build fixed** — deleted screens restored from git (login, membres, scanner, historique, tickets/vente) at their **April 21 versions**; scanner adapted to the new `ScanResponse`; `gerants` list + form components written (they never existed in git); `@zxing/ngx-scanner` 3.5.0 → 3.9.0 and `@zxing/browser` pinned 0.1.4 so `npm ci` works without `--legacy-peer-deps`. Verified: `ng build --configuration production` and `docker build` both pass.
- [x] **Backend Dockerfile** runtime image `eclipse-temurin:17-jre-alpine` → `17-jre` (alpine variant has no ARM build). `docker build` passes.
- [ ] **Re-test the restored screens by hand** — they are the April versions; any edits made to them after the last commit are lost (login, member list/detail/form, QR scanner, ticket sale). Check login works with the gérant role and new guards.
- [ ] **Secrets committed to GitHub** (`Alouvelli/metapower-gymtrack`, since commit `1132f3b`): a Gmail app password and a DB password are hard-coded as defaults in `application.yml`; the JWT secret is the same in `application.yml`, `docker-compose.yml` and `.env.example`.
  → Rotate the Gmail app password, DB password and JWT secret; remove the defaults from `application.yml` so a missing env var fails fast.
- [ ] **Commit the release.** Nothing above is in git yet — commit, tag (e.g. `v1.0.0`) and push so you deploy a known version.

## Résultats du test local (2026-10-15)

Stack complète lancée en local (Docker Compose + Mailpit), données de `seed_test_data.sql`.

**Testé OK :** login admin + gérant · liste/détail/édition membre + QR · vente ticket + reçu PDF · scan membre/ticket via API tablette (autorisé, doublon, QR invalide, sans clé → 401) · historique pointages · création gérant + modification créneau · menu et API restreints pour le gérant · CA du jour admin (93 000 FCFA attendu = obtenu).
**Non testé :** scan caméra réelle sur tablette, upload photo, exports PDF/Excel des rapports, emails de rappel.

**Corrigé pendant le test :**
- [x] nginx gardait l'IP du backend → 502 sur tout le site après un redémarrage du backend (résolveur Docker ajouté)
- [x] nginx : photos `/files/…jpg` interceptées par la règle des assets statiques (`^~` ajouté) ; upload limité à 1 Mo (→ 10 Mo)
- [x] Écrans membres restaurés lisaient `plan.nom` au lieu de `planNom` (colonne Plan vide, plan non pré-rempli)
- [x] Header/sidebar affichaient « Administrateur / Super Admin » en dur pour tout le monde
- [x] Accès refusé (gérant → API admin) renvoyait 500 au lieu de 403 ; URL inconnue 500 au lieu de 404

**Tickets par WhatsApp (ajouté) :**
- [x] Numéro WhatsApp obligatoire pour tout ticket (normalisé au format 221XXXXXXXXX) ; bouton « Envoyer le ticket sur WhatsApp » (lien wa.me, message pré-rempli, la caisse appuie sur Envoyer)
- [x] Page client publique `/t/<token>` avec le QR à présenter à l'entrée ; affiche « déjà utilisé » / « expiré »
- [x] Faille fermée : `/api/tickets/{id}/recu` était public par id séquentiel (QR valides récupérables) → reçu public uniquement par token
- [x] Numérotation des tickets : l'ancien suffixe (secondes % 10000) provoquait des doublons → compteur journalier
- [ ] Prod : tester l'envoi depuis le poste de caisse (WhatsApp Web connecté ou WhatsApp Desktop installé)

**À décider (règles métier, non modifiées) :**
- [x] **Plans multi-mois** : un paiement couvre désormais toute la durée du plan (`nb_mois` / `mois_fin`, migration **V3**). Paiements existants repris : montant = prix du plan multi-mois actuel → durée du plan, sinon 1 mois. **Vérifier ce résultat sur une copie de la base de prod avant de déployer.** Taux de recouvrement, scan et rappels email utilisent la couverture.
- [x] **Anti-doublon** : seul un scan **autorisé** verrouille le QR 5 min.
- [x] **Période « de date à date »** (migration **V4**) : payé le 29/09 → valable jusqu'au 28/10 ; renouvellement anticipé = démarre au lendemain de la fin ; rappel email 3 jours avant la fin propre à chaque membre
- [ ] Health `/actuator/health` passe DOWN si le SMTP est injoignable → vérifier `MAIL_*` en prod, ou exclure le mail du health (`management.health.mail.enabled=false`)

## Pre-Deploy

**Build & quality**
- [ ] Backend builds: `cd backend && ./mvnw clean package` (the Dockerfile uses `-DskipTests` and there is **no `src/test`** — so do it by hand)
- [ ] Frontend builds in prod mode: `cd frontend && npm ci && npm run build -- --configuration production`
- [ ] `docker compose build` succeeds on a clean checkout (not your working folder — catches files that are only on disk)
- [ ] Code reviewed (at least a self-review of the 66-file diff, focusing on `SecurityConfig`, `GerantController`, `UserController`, guards)
- [ ] No known critical bugs in: QR scan, pointage, ticket sale + PDF receipt, mensualité payment

**Database (Flyway + PostgreSQL 15)**
- [ ] `V1__init.sql` and `V2__add_gerant.sql` are in `src/main/resources/db/migration/` and committed
- [ ] Migrations tested on a **copy of production data** (not only an empty DB) — `gerant_site` table + FKs created, backend starts with `ddl-auto: validate`
- [ ] Do **not** edit an already-applied migration (Flyway checksum error) — add `V3__…` instead
- [ ] Backup taken right before deploy: `docker exec gymtrack_db pg_dump -U gymtrack gymtrack_db > backup_$(date +%F_%H%M).sql` — and a restore was tested once
- [ ] Uploaded member photos / files (`/files/`) backed up too (check where they are stored — not in a named volume in `docker-compose.yml`)
- [ ] `scripts/seed_test_data.sql` is **not** run against production

**Configuration & secrets (`.env` on the server)**
- [ ] `.env` exists on the server with real values — none left at `changeme`, `passer`, `passer123` or the published JWT secret
- [ ] `JWT_SECRET` new random 256-bit value (`openssl rand -hex 32`) — note: changing it logs everyone out
- [ ] `ADMIN_DEFAULT_PASSWORD` strong; admin password changed after first login. `DataInitializer.java:56` **logs the admin password in clear** — remove that log line or purge logs
- [ ] `DEVICE_ACCESS_KEY` set and configured on the reception tablets/scanners
- [ ] `APP_BASE_URL` = real **public HTTPS** production URL (used for CORS, email links and the **ticket link sent by WhatsApp** — `localhost` or a `192.168.x.x` LAN address won't open on the client's phone over 4G)
- [ ] `MAIL_*` set and a test expiry-reminder email received
- [ ] `environment.prod.ts` has `apiUrl: ''` (relative, through nginx) ✅ — the dev `environment.ts` points to `192.168.1.245:8090`, make sure prod build uses `--configuration production`

**Security hardening**
- [ ] Postgres port `5432` not exposed publicly (remove `ports:` from `db` or bind to `127.0.0.1`)
- [ ] Swagger (`/swagger-ui.html`, `/v3/api-docs`) disabled or protected in prod
- [ ] CORS: review `192.168.*.*` wildcards in `SecurityConfig.java` — fine for LAN, but not if the server is internet-facing
- [ ] Public endpoints reviewed: `/api/membres/*/qrcode`, `/api/tickets/*/recu`, `/files/view/**` — IDs not guessable?
- [ ] HTTPS in front of nginx (certificate valid) if accessible outside the gym LAN
- [ ] New role guards (`admin.guard.ts`, `gerant.guard.ts`) matched by backend authorization (a gérant cannot call admin APIs directly)

**People**
- [ ] Rollback plan below read and understood
- [ ] Gym managers at **both sites** told about the deploy window (pick closing hours — reception can't scan during downtime)
- [ ] Fallback at reception: paper list / manual entry during the window

## Import des membres (registre papier)

Données dans `data/import-membres/` (**hors git** — dépôt public). 283 membres, 303 paiements.
- [ ] Faire relire par le client les lignes de `registre.csv` / `paiements.csv` dont la colonne `a_verifier` est remplie (noms illisibles, numéros raturés, doublons 585/645, 663/727, 600/601) ; corriger les CSV
- [ ] Trouver le numéro de carte de **Daouda Diallo** (écrit « 37 », déjà pris par Moustapha Diouf) — non importé
- [ ] 6 membres sans téléphone (27, 111, 602, 604, 621, 772) : à compléter dans l'app
- [ ] Après la migration V4 : `python3 scripts/import_membres_papier.py > data/import-membres/import.sql`
- [ ] `docker exec -i gymtrack_db psql -U gymtrack -d gymtrack_db -v ON_ERROR_STOP=1 < data/import-membres/import.sql` (rejouable)
- [ ] Contrôle : nombre de membres = 283 de plus ; un membre payé fin septembre (ex. 779) est autorisé au scan
- [ ] Ne pas rejouer l'import après que la caisse a commencé à saisir des paiements pour les mêmes membres sans relire : les paiements « IMPORT-PAPIER » sont recréés (les paiements saisis dans l'app ne sont pas touchés)

## Deploy

- [ ] Deploy to a staging/test machine first with the prod `.env` (minus real mail) and verify
- [ ] On the server: `git pull` (tagged version) → `docker compose build` → `docker compose up -d`
- [ ] Watch backend startup: `docker compose logs -f backend` — Flyway shows migrations applied, no `SchemaManagementException`
- [ ] Health: `curl http://<server>:8080/actuator/health` → `{"status":"UP"}`
- [ ] Smoke tests (both sites):
  - [ ] Login as admin and as a gérant (gérant sees only their site)
  - [ ] Create a member → QR code generated → photo upload works
  - [ ] Record a mensualité (Espèces / Wave / Orange Money / Carte)
  - [ ] Sell an entry ticket → PDF receipt prints
  - [ ] Scan member QR **and** ticket QR at reception tablet (camera permission over HTTPS!)
  - [ ] Universal QR: a member from site A accepted at site B
  - [ ] Dashboard per site + consolidated loads; PDF and Excel export download
- [ ] Monitor logs for errors for 15 min (`docker compose logs --since 15m backend | grep -i error`)

## Post-Deploy

- [ ] Data looks right: member count, active mensualités, today's pointages match pre-deploy numbers
- [ ] Tomorrow morning: check expiry-reminder emails were sent
- [ ] Update README/changelog (gérant role, mes-transactions, multisite gérant assignment)
- [ ] Tell stakeholders (METAPOWER management, gérants) what's new
- [ ] Remove `backend/target/` and `frontend/dist/` leftovers from the server if copied

## Rollback Plan

1. `docker compose down backend frontend`
2. `git checkout <previous tag/commit>` → `docker compose build && docker compose up -d`
3. If `V2__add_gerant` was applied and the old code fails `validate`: restore the pre-deploy `pg_dump` (`psql -U gymtrack gymtrack_db < backup_….sql` into a fresh DB) — **data entered since deploy is lost**, so note any payments/tickets taken in between
4. Tell both sites; verify login + QR scan on the old version

## Rollback Triggers

- Backend doesn't reach `UP` within 5 min, or Flyway fails
- Login fails for admin or gérants
- QR scan / pointage fails at either site (business-critical — roll back immediately)
- Ticket PDF receipt or mensualité payment can't be saved
- More than a handful of 5xx errors in the first 15 min
