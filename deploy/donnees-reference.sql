-- ============================================================
-- METAPOWER GymTrack — données de référence de production
-- Plans, salles, tarifs tickets. Rejouable : ne crée que ce qui manque,
-- ne modifie jamais une ligne existante (les prix se changent ensuite dans l'appli).
-- À lancer UNE FOIS après le 1er démarrage, AVANT l'import des membres :
--   docker exec -i gymtrack_db psql -U gymtrack -d gymtrack_db -v ON_ERROR_STOP=1 < deploy/donnees-reference.sql
-- ============================================================
BEGIN;

INSERT INTO plan_abonnement (nom, duree_en_mois, prix, actif)
SELECT v.nom, v.duree, v.prix, true
FROM (VALUES ('Mensuel', 1, 20000), ('Trimestriel', 3, 55000),
             ('Semestriel', 6, 70000), ('Annuel', 12, 220000)) AS v(nom, duree, prix)
WHERE NOT EXISTS (SELECT 1 FROM plan_abonnement p WHERE p.nom = v.nom);

INSERT INTO site (nom, adresse, telephone, actif)
SELECT v.nom, v.adresse, v.tel, v.actif
FROM (VALUES ('METAPOWER FASS',  'Avenue Cheikh Anta Diop, Dakar', '+221 77 115 94 27', true),
             ('METAPOWER FOIRE', 'Foire',                          '+221 77 123 23 42', false)) AS v(nom, adresse, tel, actif)
WHERE NOT EXISTS (SELECT 1 FROM site s WHERE s.nom = v.nom);

INSERT INTO tarif_ticket (libelle, prix, duree_validite_heures, actif)
SELECT 'Séance simple', 2000, 24, true
WHERE NOT EXISTS (SELECT 1 FROM tarif_ticket t WHERE t.libelle = 'Séance simple' AND t.actif);

COMMIT;

SELECT 'plans' AS quoi, count(*) FROM plan_abonnement
UNION ALL SELECT 'salles', count(*) FROM site
UNION ALL SELECT 'tarifs actifs', count(*) FROM tarif_ticket WHERE actif;
