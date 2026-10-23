#!/usr/bin/env bash
# Déploiement / mise à jour en production.
# Usage (sur le serveur, à la racine du projet) : ./deploy/deploy.sh [branche-ou-tag]   (défaut : main)
set -euo pipefail
cd "$(dirname "$0")/.."

REF=${1:-main}
COMPOSE=(docker compose -f docker-compose.yml -f docker-compose.prod.yml)

[ -f .env ] || { echo "Pas de .env : lancer d'abord ./deploy/init-env.sh" >&2; exit 1; }

# 1. Sauvegarde de la base si elle tourne déjà
mkdir -p backups
if "${COMPOSE[@]}" ps --status running db 2>/dev/null | grep -q gymtrack_db; then
  FICHIER="backups/gymtrack_$(date +%Y%m%d_%H%M%S).sql.gz"
  "${COMPOSE[@]}" exec -T db pg_dump -U gymtrack gymtrack_db | gzip > "$FICHIER"
  echo "Sauvegarde : $FICHIER ($(du -h "$FICHIER" | cut -f1))"
  ls -1t backups/gymtrack_*.sql.gz | tail -n +31 | xargs -r rm --   # garde les 30 dernières
fi

# 2. Code
git fetch --tags origin
git checkout "$REF"
git pull --ff-only origin "$REF" 2>/dev/null || true
echo "Version : $(git log -1 --format='%h %s')"

# 3. Build + démarrage
"${COMPOSE[@]}" up -d --build

# 4. Attente du backend (migrations Flyway incluses)
echo -n "Démarrage du backend"
for _ in $(seq 1 90); do
  if "${COMPOSE[@]}" exec -T caddy wget -qO- http://backend:8080/actuator/health 2>/dev/null | grep -q '"UP"'; then
    echo " → UP"
    "${COMPOSE[@]}" ps
    echo "OK : https://$(grep '^DOMAIN=' .env | cut -d= -f2)"
    exit 0
  fi
  echo -n "."; sleep 2
done
echo " → ÉCHEC" >&2
"${COMPOSE[@]}" logs --tail 80 backend >&2
exit 1
