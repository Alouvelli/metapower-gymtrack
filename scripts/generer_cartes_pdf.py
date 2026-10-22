#!/usr/bin/env python3
"""
Génère les cartes membres en PDF prêts à imprimer, avec le design de l'appli (METACARD).

Deux fichiers dans data/cartes/ (données personnelles : dossier ignoré par git) :
  cartes_A4.pdf         une carte par page A4 paysage, identique à « Imprimer la carte » de l'appli
  cartes_format_CB.pdf  format carte bancaire 85,6 × 54 mm, 10 par A4 avec traits de coupe

Sélection : membres présents sur les feuilles de paiement (data/import-membres/paiements.csv)
+ tous les membres dont le numéro de carte est ≥ 500.

Sources :
  - membres et jetons QR : base PostgreSQL (conteneur Docker)
  - QR : endpoint public de l'appli /api/membres/{id}/qrcode (le même QR que l'écran membre)
  - styles : SCSS du composant détail membre + règles d'impression compilées (frontend/dist)

Usage :
  (cd frontend && npx ng build --configuration production)   # si dist/ absent
  python3 scripts/generer_cartes_pdf.py [--api http://localhost:4200] [--conteneur gymtrack_db]
"""
import argparse
import base64
import csv
import glob
import re
import subprocess
import sys
import tempfile
import urllib.request
from concurrent.futures import ThreadPoolExecutor
from html import escape
from pathlib import Path

RACINE = Path(__file__).resolve().parent.parent
FRONT = RACINE / 'frontend'
SORTIE = RACINE / 'data' / 'cartes'
CHROME = '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome'
NUMERO_MIN = 500

POLICES = ('<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800;900'
           '&family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet">'
           '<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css">')


def numeros_feuilles():
    with open(RACINE / 'data' / 'import-membres' / 'paiements.csv', encoding='utf-8') as f:
        return {int(r['numero']) for r in csv.DictReader(f, delimiter=';') if r['numero'].strip()}


def membres(conteneur):
    req = ("SELECT m.id, m.numero_carte, m.nom, m.prenom, m.telephone, COALESCE(p.nom, '') "
           "FROM membre m LEFT JOIN plan_abonnement p ON p.id = m.plan_id "
           "WHERE m.numero_carte ~ '^MBR-[0-9]+$' ORDER BY m.numero_carte")
    out = subprocess.run(['docker', 'exec', conteneur, 'psql', '-U', 'gymtrack', '-d', 'gymtrack_db',
                          '-tA', '-F', '\t', '-c', req], check=True, capture_output=True, text=True).stdout
    res = []
    for ligne in out.splitlines():
        i, num, nom, prenom, tel, plan = ligne.split('\t')
        res.append(dict(id=int(i), numero=num, n=int(num[4:]), nom=nom, prenom=prenom, tel=tel, plan=plan))
    return res


def qr_base64(api, membre_id):
    with urllib.request.urlopen(f'{api}/api/membres/{membre_id}/qrcode', timeout=30) as r:
        return base64.b64encode(r.read()).decode()


def css_carte():
    """Styles .cp-* du composant (le SCSS restauré est du CSS à plat)."""
    scss = (FRONT / 'src/app/features/membres/detail/membre-detail.component.scss').read_text(encoding='utf-8')
    scss = '\n'.join(l for l in scss.splitlines() if not l.lstrip().startswith('//'))
    regles = re.findall(r'([^{}]+)\{([^{}]*)\}', scss)
    return '\n'.join(f'{s.strip()} {{{b}}}' for s, b in regles if '.cp-' in s and ':host' not in s)


def css_impression_a4():
    """Règles .cp-* du bloc @media print compilé (body.printing-carte app-membre-detail .cp-…)."""
    fichiers = glob.glob(str(FRONT / 'dist/gymtrack-frontend/browser/styles*.css'))
    if not fichiers:
        sys.exit('frontend/dist absent : lancer « npx ng build --configuration production » dans frontend/')
    css = Path(fichiers[0]).read_text(encoding='utf-8')
    regles = []
    for m in re.finditer(r'@media print\s*\{', css):
        prof = 0
        for k in range(m.end() - 1, len(css)):
            prof += {'{': 1, '}': -1}.get(css[k], 0)
            if prof == 0:
                regles += [f'{s.strip()} {{{b}}}' for s, b in re.findall(r'([^{}]+)\{([^{}]*)\}', css[m.end():k])
                           if '.cp-' in s]
                break
    return '\n'.join(regles)


def carte_html(m, logo, qr):
    plan = (f'<div class="cp-plan"><i class="fas fa-star"></i><span>{escape(m["plan"])}</span></div>'
            if m['plan'] else '')
    tel = (f'<div class="cp-tel"><i class="fas fa-phone"></i><span>{escape(m["tel"])}</span></div>'
           if m['tel'] else '')
    return f'''<div class="cp-card">
  <div class="cp-bg" aria-hidden="true"><img src="{logo}" class="cp-bg-logo" alt=""><div class="cp-bg-pattern"></div></div>
  <div class="cp-stripe"></div>
  <div class="cp-header">
    <div class="cp-brand"><img src="{logo}" alt="MP" class="cp-logo-img">
      <div class="cp-brand-text"><span class="cp-brand-name">METAPOWER GYM</span><span class="cp-brand-sub">African Fitness Hardcore</span></div>
    </div>
    <div class="cp-type-badge">MEMBRE</div>
  </div>
  <div class="cp-body">
    <div class="cp-info">
      <div class="cp-name-block"><div class="cp-nom">{escape(m["nom"].upper())}</div><div class="cp-prenom">{escape(m["prenom"])}</div></div>
      <div class="cp-divider"></div>
      <div class="cp-details-block">
        <div class="cp-num"><i class="fas fa-id-card"></i><span>{m["numero"]}</span></div>{plan}{tel}
      </div>
    </div>
    <div class="cp-qr-section"><div class="cp-qr-wrap"><img src="data:image/png;base64,{qr}" alt="QR" class="cp-qr"></div></div>
  </div>
  <div class="cp-footer"><div class="cp-social"><span><i class="fab fa-instagram"></i>meta_power_gym</span><span><i class="fab fa-tiktok"></i>coach_mola</span><span><i class="fas fa-phone-volume"></i>77 300 02 74 · 71 013 07 24</span></div></div>
</div>'''


def page_a4(cartes, base_css):
    pages = ''.join(f'<section class="page"><app-membre-detail><div class="carte-preview-wrap">{c}</div></app-membre-detail></section>'
                    for c in cartes)
    return f'''<!doctype html><html lang="fr"><head><meta charset="utf-8">{POLICES}<style>
@page {{ size: A4 landscape; margin: 0; }}
* {{ -webkit-print-color-adjust: exact; print-color-adjust: exact; box-sizing: border-box; }}
body {{ margin: 0; font-family: Inter, sans-serif; }}
.page {{ width: 297mm; height: 210mm; overflow: hidden; page-break-after: always; break-after: page; }}
.page:last-child {{ page-break-after: auto; break-after: auto; }}
app-membre-detail {{ display: block; }}
.cp-qr {{ image-rendering: pixelated; }}
{base_css}
@media print {{ {css_impression_a4()} }}
</style></head><body class="printing-carte">{pages}</body></html>'''


def page_cb(cartes, base_css):
    # 85,6 × 54 mm (ISO CR80) — la carte de l'appli fait 380 × 240 px (même proportion)
    L, H, COLS, LIGNES = 85.6, 54.0, 2, 5
    mx, my = (210 - COLS * L) / 2, (297 - LIGNES * H) / 2
    echelle = (L * 96 / 25.4) / 380
    traits = ''
    for c in range(COLS + 1):
        x = mx + c * L
        traits += f'<i class="t v" style="left:{x:.2f}mm;top:2mm"></i><i class="t v" style="left:{x:.2f}mm;top:{297 - my + 2:.2f}mm"></i>'
    for r in range(LIGNES + 1):
        y = my + r * H
        traits += f'<i class="t h" style="top:{y:.2f}mm;left:2mm"></i><i class="t h" style="top:{y:.2f}mm;left:{210 - mx + 2:.2f}mm"></i>'
    pages = ''
    for i in range(0, len(cartes), COLS * LIGNES):
        cases = ''.join(
            f'<div class="case" style="left:{mx + (k % COLS) * L:.2f}mm;top:{my + (k // COLS) * H:.2f}mm">{c}</div>'
            for k, c in enumerate(cartes[i:i + COLS * LIGNES]))
        pages += f'<section class="page">{traits}{cases}</section>'
    return f'''<!doctype html><html lang="fr"><head><meta charset="utf-8">{POLICES}<style>
@page {{ size: A4 portrait; margin: 0; }}
* {{ -webkit-print-color-adjust: exact; print-color-adjust: exact; box-sizing: border-box; }}
body {{ margin: 0; font-family: Inter, sans-serif; }}
.page {{ position: relative; width: 210mm; height: 297mm; overflow: hidden; page-break-after: always; break-after: page; }}
.page:last-child {{ page-break-after: auto; break-after: auto; }}
.case {{ position: absolute; width: {L}mm; height: {H}mm; overflow: hidden; }}
.case .cp-card {{ transform: scale({echelle:.5f}); transform-origin: 0 0; box-shadow: none !important; border-radius: 0 !important; }}
.cp-qr {{ image-rendering: pixelated; }}
.t {{ position: absolute; background: #000; }}
.t.v {{ width: 0.15mm; height: {my - 4:.2f}mm; }}
.t.h {{ height: 0.15mm; width: {mx - 4:.2f}mm; }}
{base_css}
</style></head><body>{pages}</body></html>'''


def imprimer(html, pdf):
    with tempfile.TemporaryDirectory() as tmp:
        src = Path(tmp) / 'cartes.html'
        src.write_text(html, encoding='utf-8')
        subprocess.run([CHROME, '--headless=new', '--disable-gpu', '--no-pdf-header-footer',
                        f'--user-data-dir={tmp}/profil', '--virtual-time-budget=20000',
                        f'--print-to-pdf={pdf}', src.as_uri()],
                       check=True, capture_output=True, timeout=600)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--api', default='http://localhost:4200')
    ap.add_argument('--conteneur', default='gymtrack_db')
    a = ap.parse_args()

    feuilles = numeros_feuilles()
    tous = membres(a.conteneur)
    choisis = [m for m in tous if m['n'] in feuilles or m['n'] >= NUMERO_MIN]
    absents = sorted(feuilles - {m['n'] for m in tous})
    print(f'{len(choisis)} cartes ({len(tous)} membres en base) — feuilles : {len(feuilles)}, ≥ {NUMERO_MIN} : '
          f'{sum(1 for m in tous if m["n"] >= NUMERO_MIN)}', file=sys.stderr)
    if absents:
        print(f'ATTENTION numéros des feuilles absents de la base : {absents}', file=sys.stderr)

    with ThreadPoolExecutor(8) as ex:
        qrs = list(ex.map(lambda m: qr_base64(a.api, m['id']), choisis))
    logo = 'data:image/png;base64,' + base64.b64encode((FRONT / 'src/assets/images/logo.png').read_bytes()).decode()
    cartes = [carte_html(m, logo, q) for m, q in zip(choisis, qrs)]

    SORTIE.mkdir(parents=True, exist_ok=True)
    base = css_carte()
    imprimer(page_a4(cartes, base), SORTIE / 'cartes_A4.pdf')
    imprimer(page_cb(cartes, base), SORTIE / 'cartes_format_CB.pdf')
    (SORTIE / 'liste_cartes.csv').write_text(
        'numero_carte;nom;prenom;telephone;plan\n' +
        ''.join(f'{m["numero"]};{m["nom"]};{m["prenom"]};{m["tel"]};{m["plan"]}\n' for m in choisis),
        encoding='utf-8')
    print(f'→ {SORTIE}/cartes_A4.pdf, cartes_format_CB.pdf, liste_cartes.csv', file=sys.stderr)


if __name__ == '__main__':
    main()
