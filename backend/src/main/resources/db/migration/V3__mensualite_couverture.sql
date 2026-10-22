-- ============================================================
-- METAPOWER GymTrack — V3 : couverture multi-mois des paiements
-- Un paiement Trimestriel / Semestriel / Annuel couvre plusieurs
-- mois : on stocke le nombre de mois et le dernier mois couvert.
-- ============================================================

ALTER TABLE mensualite ADD COLUMN nb_mois  INT  NOT NULL DEFAULT 1;
ALTER TABLE mensualite ADD COLUMN mois_fin DATE;

-- Reprise de l'existant : un paiement dont le montant correspond exactement
-- au prix du plan multi-mois actuel du membre couvre la durée de ce plan.
-- Tous les autres paiements restent à 1 mois.
UPDATE mensualite m
SET    nb_mois = p.duree_en_mois
FROM   membre mb
JOIN   plan_abonnement p ON p.id = mb.plan_id
WHERE  mb.id = m.membre_id
  AND  p.duree_en_mois > 1
  AND  m.montant = p.prix;

UPDATE mensualite
SET    mois_fin = (mois_concerne + make_interval(months => nb_mois - 1))::date;

ALTER TABLE mensualite ALTER COLUMN mois_fin SET NOT NULL;
ALTER TABLE mensualite ADD CONSTRAINT ck_mensualite_nb_mois CHECK (nb_mois >= 1);

CREATE INDEX idx_mensualite_couverture ON mensualite (membre_id, mois_concerne, mois_fin);
