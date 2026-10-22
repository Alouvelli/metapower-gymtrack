# Déploiement en production — METAPOWER GymTrack

Installation sur un VPS Linux (Ubuntu 22.04+ ; **2 Go de RAM minimum**, le build Maven en a besoin)
avec Docker, HTTPS automatique (Caddy + Let's Encrypt) et base PostgreSQL non exposée.

## 0. Avant de commencer (à faire soi-même)

- [ ] **Révoquer l'ancien mot de passe d'application Gmail** (il est public sur GitHub) :
      compte Google → Sécurité → Mots de passe des applications → supprimer, puis en créer un nouveau
- [ ] Accès SSH au VPS ; ports **80 et 443** ouverts chez l'hébergeur
- [ ] La PR est mergée dans `main` (le script déploie `main` par défaut)
- [ ] Sans nom de domaine : l'adresse sera `https://<ip-avec-tirets>.sslip.io` (gratuit, certificat valide).
      Avec un domaine : faire pointer son enregistrement DNS A vers l'IP du VPS

## 1. Préparer le serveur (une fois)

```bash
sudo apt update && sudo apt install -y docker.io docker-compose-v2 git
docker compose version          # doit afficher 2.24 ou plus (sinon : docs.docker.com/engine/install/ubuntu)
sudo usermod -aG docker $USER   # puis se déconnecter / reconnecter
sudo ufw allow OpenSSH && sudo ufw allow 80 && sudo ufw allow 443 && sudo ufw --force enable
sudo git clone https://github.com/Alouvelli/metapower-gymtrack.git /opt/gymtrack
sudo chown -R $USER /opt/gymtrack && cd /opt/gymtrack
```

## 2. Configurer et démarrer

```bash
./deploy/init-env.sh      # crée .env (secrets aléatoires), affiche l'adresse et le mot de passe admin
./deploy/deploy.sh        # build + démarrage + attente « UP » (5 à 10 min la 1re fois)
```

## 3. Données (une fois)

```bash
docker exec -i gymtrack_db psql -U gymtrack -d gymtrack_db -v ON_ERROR_STOP=1 < deploy/donnees-reference.sql
```

Les membres du registre papier **ne sont pas dans git** (données personnelles, dépôt public).
Depuis le Mac, après relecture des CSV par le client :

```bash
python3 scripts/import_membres_papier.py > data/import-membres/import.sql
scp data/import-membres/import.sql <user>@<ip-vps>:/opt/gymtrack/backups/import.sql
```

Puis sur le serveur :

```bash
docker exec -i gymtrack_db psql -U gymtrack -d gymtrack_db -v ON_ERROR_STOP=1 < backups/import.sql
rm backups/import.sql
```

### Cartes membres déjà imprimées

Les QR des cartes viennent de `data/import-membres/qr_tokens.csv` (hors git). **Ne jamais supprimer
ce fichier** : l'import l'utilise pour donner en production les mêmes QR que les cartes imprimées.
Si le fichier était perdu, les cartes imprimées seraient refusées à l'entrée.
Le garder avec les sauvegardes (il est dans `import.sql`, envoyé sur le serveur à l'étape ci-dessus).

Générer les cartes (sur le Mac, base locale à jour) :

```bash
python3 scripts/generer_cartes_pdf.py   # → data/cartes/cartes_A4.pdf, cartes_format_CB.pdf
```

## 4. Mise en service

- [ ] Se connecter avec `admin` et **changer le mot de passe**
- [ ] Vérifier les prix (Paramètres / Tarifs) et les salles
- [ ] Tablettes d'accueil : ouvrir `https://<adresse>/scanner`, choisir la salle, saisir `DEVICE_ACCESS_KEY` (dans `.env`)
- [ ] Vendre un ticket test avec son propre numéro WhatsApp → ouvrir le lien sur le téléphone → scanner

## Mises à jour

```bash
cd /opt/gymtrack && ./deploy/deploy.sh          # sauvegarde la base, met à jour, redémarre
```

## Sauvegardes et restauration

`deploy.sh` sauvegarde la base dans `backups/` avant chaque mise à jour (30 dernières conservées).
Pour une sauvegarde quotidienne : `crontab -e` →
`0 3 * * * cd /opt/gymtrack && docker exec gymtrack_db pg_dump -U gymtrack gymtrack_db | gzip > backups/quotidien_$(date +\%u).sql.gz`

Restauration (efface les données actuelles) :

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml stop backend
gunzip -c backups/<fichier>.sql.gz | docker exec -i gymtrack_db psql -U gymtrack -d gymtrack_db
docker compose -f docker-compose.yml -f docker-compose.prod.yml start backend
```

**Copier régulièrement `backups/` hors du VPS** (une panne du serveur emporte aussi les sauvegardes locales).
