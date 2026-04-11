-- ============================================================
-- METAPOWER GymTrack — Schéma initial (V1)
-- Multisite, membres, mensualités, tickets non-membres, pointages
-- ============================================================

-- ─── Sécurité / Utilisateurs ──────────────────────────────────
CREATE TABLE app_role (
    id        BIGSERIAL PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE app_user (
    id                          BIGSERIAL PRIMARY KEY,
    username                    VARCHAR(80)  NOT NULL UNIQUE,
    password                    VARCHAR(255) NOT NULL,
    email                       VARCHAR(120) UNIQUE,
    nom                         VARCHAR(80),
    prenom                      VARCHAR(80),
    enabled                     BOOLEAN      NOT NULL DEFAULT TRUE,
    reset_password_token        VARCHAR(255) UNIQUE,
    reset_password_expires_at   TIMESTAMP
);

CREATE TABLE app_user_role (
    user_id BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES app_role(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- ─── Multisite ────────────────────────────────────────────────
CREATE TABLE site (
    id         BIGSERIAL PRIMARY KEY,
    nom        VARCHAR(120) NOT NULL,
    adresse    VARCHAR(255) NOT NULL,
    telephone  VARCHAR(30),
    email      VARCHAR(120),
    actif      BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE TABLE staff_site (
    user_id BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    site_id BIGINT NOT NULL REFERENCES site(id)     ON DELETE CASCADE,
    PRIMARY KEY (user_id, site_id)
);

-- ─── Plans d'abonnement ───────────────────────────────────────
CREATE TABLE plan_abonnement (
    id            BIGSERIAL PRIMARY KEY,
    nom           VARCHAR(80) NOT NULL,
    duree_en_mois INT         NOT NULL,
    prix          INT         NOT NULL,
    description   VARCHAR(255),
    actif         BOOLEAN     NOT NULL DEFAULT TRUE
);

-- ─── Membres ──────────────────────────────────────────────────
CREATE TABLE membre (
    id              BIGSERIAL PRIMARY KEY,
    numero_carte    VARCHAR(30)  NOT NULL UNIQUE,
    nom             VARCHAR(80)  NOT NULL,
    prenom          VARCHAR(80)  NOT NULL,
    telephone       VARCHAR(30)  NOT NULL,
    email           VARCHAR(120),
    date_naissance  DATE,
    adresse         VARCHAR(255),
    photo_url       VARCHAR(255),
    qr_code_token   VARCHAR(64)  NOT NULL UNIQUE,
    statut          VARCHAR(20)  NOT NULL DEFAULT 'ACTIF',
    plan_id         BIGINT REFERENCES plan_abonnement(id),
    created_at      TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_membre_qr_token     ON membre (qr_code_token);
CREATE INDEX idx_membre_numero_carte ON membre (numero_carte);

-- ─── Mensualités ──────────────────────────────────────────────
CREATE TABLE mensualite (
    id                  BIGSERIAL PRIMARY KEY,
    membre_id           BIGINT      NOT NULL REFERENCES membre(id) ON DELETE CASCADE,
    mois_concerne       DATE        NOT NULL,
    montant             INT         NOT NULL,
    date_paiement       DATE,
    statut              VARCHAR(20) NOT NULL DEFAULT 'EN_ATTENTE',
    mode_paiement       VARCHAR(20),
    reference           VARCHAR(100),
    enregistre_par_id   BIGINT REFERENCES app_user(id),
    created_at          TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE INDEX idx_mensualite_membre_mois ON mensualite (membre_id, mois_concerne);

-- ─── Tarifs tickets non-membres ───────────────────────────────
CREATE TABLE tarif_ticket (
    id                    BIGSERIAL PRIMARY KEY,
    libelle               VARCHAR(80) NOT NULL,
    prix                  INT         NOT NULL,
    duree_validite_heures INT         NOT NULL DEFAULT 24,
    actif                 BOOLEAN     NOT NULL DEFAULT TRUE
);

-- ─── Tickets d'entrée vendus ──────────────────────────────────
CREATE TABLE ticket_entree (
    id               BIGSERIAL PRIMARY KEY,
    numero           VARCHAR(30)  NOT NULL UNIQUE,
    qr_code_token    VARCHAR(64)  NOT NULL UNIQUE,
    site_id          BIGINT       NOT NULL REFERENCES site(id),
    tarif_id         BIGINT REFERENCES tarif_ticket(id),
    prix             INT          NOT NULL,
    mode_paiement    VARCHAR(20)  NOT NULL,
    date_paiement    TIMESTAMP    NOT NULL DEFAULT now(),
    date_validite    TIMESTAMP    NOT NULL,
    statut           VARCHAR(20)  NOT NULL DEFAULT 'VALIDE',
    nom_client       VARCHAR(120),
    telephone_client VARCHAR(30),
    vendeur_id       BIGINT REFERENCES app_user(id),
    date_utilisation TIMESTAMP,
    created_at       TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_ticket_qr_token ON ticket_entree (qr_code_token);
CREATE INDEX idx_ticket_numero   ON ticket_entree (numero);

-- ─── Pointages (membres + tickets) ────────────────────────────
CREATE TABLE pointage (
    id            BIGSERIAL PRIMARY KEY,
    membre_id     BIGINT REFERENCES membre(id),
    ticket_id     BIGINT REFERENCES ticket_entree(id),
    site_id       BIGINT      NOT NULL REFERENCES site(id),
    heure_entree  TIMESTAMP   NOT NULL DEFAULT now(),
    heure_sortie  TIMESTAMP,
    statut        VARCHAR(20) NOT NULL,
    motif_refus   VARCHAR(60),
    scan_source   VARCHAR(20) NOT NULL DEFAULT 'TABLETTE',
    type_acces    VARCHAR(10) NOT NULL,
    valide_par_id BIGINT REFERENCES app_user(id),
    created_at    TIMESTAMP   NOT NULL DEFAULT now(),
    CONSTRAINT chk_pointage_acteur CHECK (
        (membre_id IS NOT NULL AND ticket_id IS NULL)
     OR (ticket_id IS NOT NULL AND membre_id IS NULL)
    )
);

CREATE INDEX idx_pointage_membre_heure ON pointage (membre_id, heure_entree);
CREATE INDEX idx_pointage_site_heure   ON pointage (site_id, heure_entree);
