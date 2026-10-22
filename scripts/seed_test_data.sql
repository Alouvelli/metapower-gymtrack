-- ============================================================
-- METAPOWER GymTrack — Données de test (idempotent)
-- Exécuter UNE FOIS dans pgAdmin > Query Tool, ou psql
-- Compatible PostgreSQL — sûr à relancer (rien ne se duplique)
-- ============================================================

-- ─────────────────────────────────────────────────────────────
-- 1. SITE
-- ─────────────────────────────────────────────────────────────
INSERT INTO site (nom, adresse, telephone, email, actif)
SELECT 'METAPOWER Dakar', 'Avenue Cheikh Anta Diop, Dakar', '+221 77 000 00 00', 'dakar@metapower.sn', true
WHERE NOT EXISTS (SELECT 1 FROM site WHERE nom = 'METAPOWER Dakar');

-- ─────────────────────────────────────────────────────────────
-- 2. PLANS D'ABONNEMENT
-- ─────────────────────────────────────────────────────────────
INSERT INTO plan_abonnement (nom, duree_en_mois, prix, description, actif)
SELECT 'Mensuel', 1, 25000, 'Accès illimité — 1 mois', true
WHERE NOT EXISTS (SELECT 1 FROM plan_abonnement WHERE nom = 'Mensuel');

INSERT INTO plan_abonnement (nom, duree_en_mois, prix, description, actif)
SELECT 'Trimestriel', 3, 65000, 'Accès illimité — 3 mois', true
WHERE NOT EXISTS (SELECT 1 FROM plan_abonnement WHERE nom = 'Trimestriel');

INSERT INTO plan_abonnement (nom, duree_en_mois, prix, description, actif)
SELECT 'Semestriel', 6, 110000, 'Accès illimité — 6 mois', true
WHERE NOT EXISTS (SELECT 1 FROM plan_abonnement WHERE nom = 'Semestriel');

INSERT INTO plan_abonnement (nom, duree_en_mois, prix, description, actif)
SELECT 'Annuel', 12, 200000, 'Accès illimité — 12 mois', true
WHERE NOT EXISTS (SELECT 1 FROM plan_abonnement WHERE nom = 'Annuel');

-- ─────────────────────────────────────────────────────────────
-- 3. TARIFS TICKETS (non-membres)
-- ─────────────────────────────────────────────────────────────
INSERT INTO tarif_ticket (libelle, prix, duree_validite_heures, actif)
SELECT 'Séance simple', 3000, 12, true
WHERE NOT EXISTS (SELECT 1 FROM tarif_ticket WHERE libelle = 'Séance simple');

INSERT INTO tarif_ticket (libelle, prix, duree_validite_heures, actif)
SELECT 'Séance découverte', 2000, 24, true
WHERE NOT EXISTS (SELECT 1 FROM tarif_ticket WHERE libelle = 'Séance découverte');

-- ─────────────────────────────────────────────────────────────
-- 4. MEMBRES (8 membres — statuts variés pour tester tous les cas)
--    qr_code_token = UUID unique utilisé pour le scan tablette
-- ─────────────────────────────────────────────────────────────
INSERT INTO membre (numero_carte, nom, prenom, telephone, email, statut, plan_id, qr_code_token)
SELECT 'MBR-2026-0001','DIALLO','Mamadou','+221 77 111 00 01','mamadou.diallo@test.sn',
       'ACTIF',(SELECT id FROM plan_abonnement WHERE nom='Mensuel'),
       'a1000001-0001-0001-0001-000000000001'
WHERE NOT EXISTS (SELECT 1 FROM membre WHERE numero_carte='MBR-2026-0001');

INSERT INTO membre (numero_carte, nom, prenom, telephone, email, statut, plan_id, qr_code_token)
SELECT 'MBR-2026-0002','SALL','Fatou','+221 77 111 00 02','fatou.sall@test.sn',
       'ACTIF',(SELECT id FROM plan_abonnement WHERE nom='Trimestriel'),
       'a1000002-0002-0002-0002-000000000002'
WHERE NOT EXISTS (SELECT 1 FROM membre WHERE numero_carte='MBR-2026-0002');

INSERT INTO membre (numero_carte, nom, prenom, telephone, email, statut, plan_id, qr_code_token)
SELECT 'MBR-2026-0003','NDIAYE','Ibrahima','+221 77 111 00 03','ibrahima.ndiaye@test.sn',
       'ACTIF',(SELECT id FROM plan_abonnement WHERE nom='Mensuel'),
       'a1000003-0003-0003-0003-000000000003'
WHERE NOT EXISTS (SELECT 1 FROM membre WHERE numero_carte='MBR-2026-0003');

INSERT INTO membre (numero_carte, nom, prenom, telephone, email, statut, plan_id, qr_code_token)
SELECT 'MBR-2026-0004','BA','Aminata','+221 77 111 00 04','aminata.ba@test.sn',
       'ACTIF',(SELECT id FROM plan_abonnement WHERE nom='Annuel'),
       'a1000004-0004-0004-0004-000000000004'
WHERE NOT EXISTS (SELECT 1 FROM membre WHERE numero_carte='MBR-2026-0004');

INSERT INTO membre (numero_carte, nom, prenom, telephone, email, statut, plan_id, qr_code_token)
SELECT 'MBR-2026-0005','FALL','Omar','+221 77 111 00 05','omar.fall@test.sn',
       'ACTIF',(SELECT id FROM plan_abonnement WHERE nom='Semestriel'),
       'a1000005-0005-0005-0005-000000000005'
WHERE NOT EXISTS (SELECT 1 FROM membre WHERE numero_carte='MBR-2026-0005');

INSERT INTO membre (numero_carte, nom, prenom, telephone, email, statut, plan_id, qr_code_token)
SELECT 'MBR-2026-0006','CAMARA','Aissatou','+221 77 111 00 06','aissatou.camara@test.sn',
       'ACTIF',(SELECT id FROM plan_abonnement WHERE nom='Mensuel'),
       'a1000006-0006-0006-0006-000000000006'
WHERE NOT EXISTS (SELECT 1 FROM membre WHERE numero_carte='MBR-2026-0006');

-- Membre EXPIRE — pour tester le refus au scan
INSERT INTO membre (numero_carte, nom, prenom, telephone, email, statut, plan_id, qr_code_token)
SELECT 'MBR-2026-0007','DIOUF','Cheikh','+221 77 111 00 07','cheikh.diouf@test.sn',
       'EXPIRE',(SELECT id FROM plan_abonnement WHERE nom='Mensuel'),
       'a1000007-0007-0007-0007-000000000007'
WHERE NOT EXISTS (SELECT 1 FROM membre WHERE numero_carte='MBR-2026-0007');

INSERT INTO membre (numero_carte, nom, prenom, telephone, email, statut, plan_id, qr_code_token)
SELECT 'MBR-2026-0008','MBAYE','Rokhaya','+221 77 111 00 08','rokhaya.mbaye@test.sn',
       'ACTIF',(SELECT id FROM plan_abonnement WHERE nom='Trimestriel'),
       'a1000008-0008-0008-0008-000000000008'
WHERE NOT EXISTS (SELECT 1 FROM membre WHERE numero_carte='MBR-2026-0008');

-- ─────────────────────────────────────────────────────────────
-- 5. MENSUALITÉS MAI 2026
--    Membres 1-6, 8 → PAYE (accès autorisé au pointage)
--    Membre 7       → EN_ATTENTE avril (accès refusé)
-- ─────────────────────────────────────────────────────────────
INSERT INTO mensualite (membre_id, mois_concerne, montant, date_paiement, statut, mode_paiement, enregistre_par_id)
SELECT m.id, '2026-05-01'::date, pa.prix, '2026-05-01'::date, 'PAYE', 'ESPECES',
       (SELECT id FROM app_user WHERE username = 'admin')
FROM membre m
JOIN plan_abonnement pa ON pa.id = m.plan_id
WHERE m.numero_carte IN (
    'MBR-2026-0001','MBR-2026-0002','MBR-2026-0003',
    'MBR-2026-0004','MBR-2026-0005','MBR-2026-0006','MBR-2026-0008'
)
AND NOT EXISTS (
    SELECT 1 FROM mensualite mx
    WHERE mx.membre_id = m.id AND mx.mois_concerne = '2026-05-01'
);

-- Membre 7 : mensualité avril non payée (simule abonnement expiré)
INSERT INTO mensualite (membre_id, mois_concerne, montant, date_paiement, statut, mode_paiement)
SELECT m.id, '2026-04-01'::date, pa.prix, NULL, 'EN_ATTENTE', NULL
FROM membre m
JOIN plan_abonnement pa ON pa.id = m.plan_id
WHERE m.numero_carte = 'MBR-2026-0007'
AND NOT EXISTS (
    SELECT 1 FROM mensualite mx
    WHERE mx.membre_id = m.id AND mx.mois_concerne = '2026-04-01'
);

-- ─────────────────────────────────────────────────────────────
-- RÉSUMÉ
-- ─────────────────────────────────────────────────────────────
SELECT 'Sites'              AS table_name, count(*) FROM site
UNION ALL
SELECT 'Plans',               count(*) FROM plan_abonnement
UNION ALL
SELECT 'Tarifs tickets',      count(*) FROM tarif_ticket
UNION ALL
SELECT 'Membres',             count(*) FROM membre
UNION ALL
SELECT 'Mensualités',         count(*) FROM mensualite;
