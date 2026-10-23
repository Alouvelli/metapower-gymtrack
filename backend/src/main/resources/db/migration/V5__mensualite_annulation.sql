-- ============================================================
-- METAPOWER GymTrack — V5 : annulation d'un paiement (admin)
-- Un paiement saisi par erreur n'est pas supprimé : il passe au statut
-- ANNULE avec la trace de qui l'a annulé, quand et pourquoi. Il ne compte
-- plus ni pour l'accès à la salle, ni dans le chiffre d'affaires.
-- ============================================================

ALTER TABLE mensualite ADD COLUMN annule_le        TIMESTAMP;
ALTER TABLE mensualite ADD COLUMN annule_par_id    BIGINT REFERENCES app_user(id);
ALTER TABLE mensualite ADD COLUMN motif_annulation VARCHAR(255);
