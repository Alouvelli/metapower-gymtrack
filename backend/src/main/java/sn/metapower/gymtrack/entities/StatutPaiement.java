package sn.metapower.gymtrack.entities;

public enum StatutPaiement {
    PAYE, EN_ATTENTE, EN_RETARD,
    /** Paiement saisi par erreur, annulé par un admin (conservé pour la traçabilité). */
    ANNULE
}
