#!/usr/bin/env python3
"""
Import des membres et paiements du registre papier → SQL PostgreSQL idempotent.

Entrées (données clients, NON versionnées — dossier data/ ignoré par git) :
  data/import-membres/registre.csv   numero;nom_complet;telephone;date_inscription;a_verifier
  data/import-membres/paiements.csv  numero_ecrit;numero;nom_complet;telephone;date_paiement;a_verifier

Règles :
  - numéro de carte = numéro papier : 575 → MBR-0575
  - membres = registre + membres présents uniquement sur les feuilles de paiement
  - plan Mensuel pour tous ; statut ACTIF ; QR généré (gen_random_uuid)
  - mensualités PAYÉES : mois d'inscription + chaque ligne des feuilles de paiement
    (mois concerné = mois de la date de paiement ; un seul paiement par membre et par mois)
  - lignes sans numéro de carte (colonne « numero » vide) : ignorées et listées
  - rejouable : ON CONFLICT / NOT EXISTS, rien n'est dupliqué

Usage :
  python3 scripts/import_membres_papier.py > data/import-membres/import.sql
  psql -U gymtrack -d gymtrack_db -v ON_ERROR_STOP=1 -f data/import-membres/import.sql
"""
import csv
import re
import sys
from datetime import datetime
from pathlib import Path

DATA = Path(__file__).resolve().parent.parent / 'data' / 'import-membres'
REFERENCE = 'IMPORT-PAPIER'

# Noms écrits « Nom Prénom » ou avec civilité : (prenom, nom) explicites
NOMS_FORCES = {
    572: ('Oumy', 'Diagne'),
    599: ('Jean', 'Touré'),
}


def lire(fichier):
    with open(DATA / fichier, encoding='utf-8') as f:
        return list(csv.DictReader(f, delimiter=';'))


def date_fr(s):
    return datetime.strptime(s.strip(), '%d/%m/%Y').date()


def telephone(s):
    chiffres = re.sub(r'\D', '', s or '')
    if not chiffres:
        return ''
    if len(chiffres) != 9:
        raise ValueError(f'téléphone invalide : {s!r}')
    return '+221 ' + ' '.join([chiffres[:2], chiffres[2:5], chiffres[5:7], chiffres[7:]])


def prenom_nom(numero, complet):
    if numero in NOMS_FORCES:
        return NOMS_FORCES[numero]
    mots = complet.split()
    if len(mots) == 1:
        return '', mots[0]
    return ' '.join(mots[:-1]), mots[-1]


def sql(s):
    return "'" + str(s).replace("'", "''") + "'"


def main():
    membres = {}      # numero -> dict
    paiements = {}    # (numero, mois) -> date
    ignores = []

    for r in lire('registre.csv'):
        n = int(r['numero'])
        membres[n] = {'complet': r['nom_complet'].strip(), 'tel': telephone(r['telephone']),
                      'inscription': date_fr(r['date_inscription'])}
        d = membres[n]['inscription']
        paiements.setdefault((n, d.replace(day=1)), d)

    for r in lire('paiements.csv'):
        if not r['numero'].strip():
            ignores.append(r)
            continue
        n = int(r['numero'])
        d = date_fr(r['date_paiement'])
        if n not in membres:
            membres[n] = {'complet': r['nom_complet'].strip(), 'tel': telephone(r['telephone']),
                          'inscription': d}
        else:
            membres[n]['inscription'] = min(membres[n]['inscription'], d)
        mois = d.replace(day=1)
        paiements[(n, mois)] = min(paiements.get((n, mois), d), d)

    out = sys.stdout
    out.write('-- Généré par scripts/import_membres_papier.py — ne pas éditer à la main\n')
    out.write('BEGIN;\n\n')
    out.write("DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM plan_abonnement WHERE nom = 'Mensuel') THEN "
              "RAISE EXCEPTION 'Plan « Mensuel » introuvable'; END IF; END $$;\n\n")

    out.write('-- Membres\n')
    for n in sorted(membres):
        m = membres[n]
        prenom, nom = prenom_nom(n, m['complet'])
        out.write(
            'INSERT INTO membre (numero_carte, nom, prenom, telephone, qr_code_token, statut, plan_id, created_at, updated_at)\n'
            f"SELECT {sql(f'MBR-{n:04d}')}, {sql(nom.upper())}, {sql(prenom)}, {sql(m['tel'])}, "
            f"gen_random_uuid()::text, 'ACTIF', (SELECT id FROM plan_abonnement WHERE nom = 'Mensuel'), "
            f"{sql(m['inscription'].isoformat())}, now()\n"
            'ON CONFLICT (numero_carte) DO NOTHING;\n')

    out.write('\n-- Mensualités payées\n')
    for (n, mois), d in sorted(paiements.items()):
        out.write(
            'INSERT INTO mensualite (membre_id, mois_concerne, nb_mois, mois_fin, montant, date_paiement, statut, mode_paiement, reference)\n'
            f"SELECT m.id, {sql(mois.isoformat())}, 1, {sql(mois.isoformat())}, p.prix, {sql(d.isoformat())}, "
            f"'PAYE', 'ESPECES', {sql(REFERENCE)}\n"
            "FROM membre m JOIN plan_abonnement p ON p.nom = 'Mensuel'\n"
            f"WHERE m.numero_carte = {sql(f'MBR-{n:04d}')}\n"
            '  AND NOT EXISTS (SELECT 1 FROM mensualite x WHERE x.membre_id = m.id '
            f"AND x.mois_concerne <= {sql(mois.isoformat())} AND x.mois_fin >= {sql(mois.isoformat())});\n")

    out.write('\nCOMMIT;\n')

    print(f'-- {len(membres)} membres, {len(paiements)} mensualités', file=sys.stderr)
    for r in ignores:
        print(f"-- IGNORÉ (numéro à confirmer) : {r['numero_ecrit']} {r['nom_complet']} {r['date_paiement']}",
              file=sys.stderr)


if __name__ == '__main__':
    main()
