#!/usr/bin/env bash
# Crée le fichier .env de production avec des secrets aléatoires (1re installation uniquement).
# Usage (sur le serveur, à la racine du projet) : ./deploy/init-env.sh
set -euo pipefail
cd "$(dirname "$0")/.."

if [ -f .env ]; then
  echo ".env existe déjà — rien n'est modifié." >&2
  exit 1
fi

IP=$(curl -fsS https://api.ipify.org 2>/dev/null || hostname -I | awk '{print $1}')
DOMAINE_DEFAUT="$(echo "$IP" | tr '.' '-').sslip.io"

read -r -p "Domaine public [${DOMAINE_DEFAUT}] : " DOMAIN
DOMAIN=${DOMAIN:-$DOMAINE_DEFAUT}
read -r -p "Adresse Gmail d'envoi des rappels : " MAIL_USERNAME
read -r -s -p "Mot de passe d'application Gmail (nouveau) : " MAIL_PASSWORD; echo

ADMIN_PASSWORD="$(openssl rand -base64 18 | tr -d '/+=' | cut -c1-16)"

umask 077
cat > .env <<EOF
DB_PASSWORD=$(openssl rand -hex 24)
JWT_SECRET=$(openssl rand -hex 32)
ADMIN_DEFAULT_PASSWORD=${ADMIN_PASSWORD}
DEVICE_ACCESS_KEY=$(openssl rand -hex 16)
MAIL_HOST=smtp.gmail.com
MAIL_USERNAME=${MAIL_USERNAME}
MAIL_PASSWORD=${MAIL_PASSWORD}
DOMAIN=${DOMAIN}
APP_BASE_URL=https://${DOMAIN}
EOF

echo
echo ".env créé (lisible par root uniquement)."
echo "Adresse de l'appli : https://${DOMAIN}"
echo "Compte admin       : admin / ${ADMIN_PASSWORD}   ← à noter, puis à changer après la 1re connexion"
echo "Clé des tablettes  : voir DEVICE_ACCESS_KEY dans .env"
