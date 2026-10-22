-- ============================================================
-- METAPOWER GymTrack — V2 : Gérants multi-site avec créneaux
-- ============================================================

-- Nouveau rôle gérant (idempotent)
INSERT INTO app_role (role_name)
VALUES ('ROLE_GERANT')
ON CONFLICT (role_name) DO NOTHING;

-- Table de liaison gérant ↔ site avec plage horaire journalière
CREATE TABLE gerant_site (
    id          BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    site_id     BIGINT       NOT NULL REFERENCES site(id)     ON DELETE CASCADE,
    heure_debut TIME         NOT NULL,
    heure_fin   TIME         NOT NULL,
    actif       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT uq_gerant_site UNIQUE (user_id, site_id)
);

CREATE INDEX idx_gerant_site_user ON gerant_site (user_id);
CREATE INDEX idx_gerant_site_site ON gerant_site (site_id);
