-- ============================================================
-- METAPOWER GymTrack — V4 : période de validité « de date à date »
-- Un paiement couvre désormais une période qui démarre au paiement
-- (ou à la fin de l'abonnement en cours) : payé le 29/09 → valable
-- jusqu'au 28/10. mois_concerne / mois_fin restent pour l'affichage.
-- ============================================================

ALTER TABLE mensualite ADD COLUMN date_debut DATE;
ALTER TABLE mensualite ADD COLUMN date_fin   DATE;

-- Reprise de l'existant : la période calendaire déjà enregistrée
-- (1er du premier mois → dernier jour du dernier mois couvert)
UPDATE mensualite
SET    date_debut = mois_concerne,
       date_fin   = (mois_fin + INTERVAL '1 month' - INTERVAL '1 day')::date;

ALTER TABLE mensualite ALTER COLUMN date_debut SET NOT NULL;
ALTER TABLE mensualite ALTER COLUMN date_fin   SET NOT NULL;
ALTER TABLE mensualite ADD CONSTRAINT ck_mensualite_periode CHECK (date_fin >= date_debut);

CREATE INDEX idx_mensualite_periode ON mensualite (membre_id, date_debut, date_fin);
