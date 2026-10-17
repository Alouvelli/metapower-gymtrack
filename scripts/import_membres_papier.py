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
  - mensualités PAYÉES : inscription + chaque ligne des feuilles de paiement ;
    période « de date à date » comme dans l'app : payé le 29/09 → valable jusqu'au 28/10,
    un paiement fait avant la fin de la période en cours démarre au lendemain de celle-ci ;
    deux paiements à moins de 15 jours d'écart = le même paiement (ex. inscription + feuille)
  - lignes sans numéro de carte (colonne « numero » vide) : ignorées et listées
  - rejouable : membres ON CONFLICT DO NOTHING ; paiements importés (référence IMPORT-PAPIER)
    supprimés puis recréés à chaque exécution

Usage :
  python3 scripts/import_membres_papier.py > data/import-membres/import.sql
  psql -U gymtrack -d gymtrack_db -v ON_ERROR_STOP=1 -f data/import-membres/import.sql
"""
import calendar
import csv
import re
import sys
from datetime import datetime, timedelta
from pathlib import Path

DATA = Path(__file__).resolve().parent.parent / 'data' / 'import-membres'
REFERENCE = 'IMPORT-PAPIER'
MEME_PAIEMENT_JOURS = 15

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


def plus_un_mois(d):
    """Même jour le mois suivant (borné à la fin du mois), comme LocalDate.plusMonths(1)."""
    annee, mois = (d.year + 1, 1) if d.month == 12 else (d.year, d.month + 1)
    return d.replace(year=annee, month=mois, day=min(d.day, calendar.monthrange(annee, mois)[1]))


def periodes(dates):
    """Dates de paiement d'un membre → [(date_paiement, debut, fin)], chaînées comme dans l'app."""
    retenues = []
    for d in sorted(set(dates)):
        if retenues and (d - retenues[-1]).days < MEME_PAIEMENT_JOURS:
            continue
        retenues.append(d)
    res, fin_prec = [], None
    for d in retenues:
        debut = max(d, fin_prec + timedelta(days=1)) if fin_prec else d
        fin = plus_un_mois(debut) - timedelta(days=1)
        res.append((d, debut, fin))
        fin_prec = fin
    return res


def sql(s):
    return "'" + str(s).replace("'", "''") + "'"


def main():
    membres = {}      # numero -> dict
    paiements = {}    # numero -> [dates de paiement]
    ignores = []

    for r in lire('registre.csv'):
        n = int(r['numero'])
        membres[n] = {'complet': r['nom_complet'].strip(), 'tel': telephone(r['telephone']),
                      'inscription': date_fr(r['date_inscription'])}
        paiements.setdefault(n, []).append(membres[n]['inscription'])

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
        paiements.setdefault(n, []).append(d)

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

    out.write('\n-- Mensualités payées (supprimées puis recréées : script rejouable)\n')
    out.write(f"DELETE FROM mensualite WHERE reference = {sql(REFERENCE)};\n")
    nb = 0
    for n in sorted(paiements):
        for d, debut, fin in periodes(paiements[n]):
            nb += 1
            out.write(
                'INSERT INTO mensualite (membre_id, mois_concerne, nb_mois, mois_fin, date_debut, date_fin, '
                'montant, date_paiement, statut, mode_paiement, reference)\n'
                f"SELECT m.id, {sql(debut.replace(day=1).isoformat())}, 1, {sql(fin.replace(day=1).isoformat())}, "
                f"{sql(debut.isoformat())}, {sql(fin.isoformat())}, p.prix, {sql(d.isoformat())}, "
                f"'PAYE', 'ESPECES', {sql(REFERENCE)}\n"
                "FROM membre m JOIN plan_abonnement p ON p.nom = 'Mensuel'\n"
                f"WHERE m.numero_carte = {sql(f'MBR-{n:04d}')};\n")

    out.write('\nCOMMIT;\n')

    print(f'-- {len(membres)} membres, {nb} mensualités', file=sys.stderr)
    for r in ignores:
        print(f"-- IGNORÉ (numéro à confirmer) : {r['numero_ecrit']} {r['nom_complet']} {r['date_paiement']}",
              file=sys.stderr)


if __name__ == '__main__':
    main()
