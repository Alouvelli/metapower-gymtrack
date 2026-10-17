export interface Site {
  id?: number;
  nom: string;
  adresse?: string;
  telephone?: string;
  email?: string;
  actif?: boolean;
}

export interface PlanAbonnement {
  id?: number;
  nom: string;
  dureeEnMois: number;
  prix: number;
  actif?: boolean;
}

export type StatutMembre = 'ACTIF' | 'EXPIRE' | 'SUSPENDU' | 'EN_RETARD';

export interface Membre {
  id?: number;
  numeroCarte?: string;
  nom: string;
  prenom: string;
  telephone: string;
  email?: string;
  dateNaissance?: string;
  photoUrl?: string;
  qrCodeToken?: string;
  statut?: StatutMembre;
  plan?: PlanAbonnement;
  planId?: number;
  planNom?: string;
  planPrix?: number;
}

export type ModePaiement = 'ESPECES' | 'WAVE' | 'ORANGE_MONEY' | 'CARTE' | 'VIREMENT';
export type StatutPaiement = 'PAYE' | 'EN_ATTENTE' | 'EN_RETARD';

export interface Mensualite {
  id?: number;
  membre?: Membre;
  membreId?: number;
  moisConcerne: string;
  moisFin?: string;       // dernier mois couvert (plans multi-mois)
  nbMois?: number;
  dateDebut: string;      // premier jour couvert
  dateFin: string;        // dernier jour couvert (payé le 29/09 → 28/10)
  montant: number;
  datePaiement?: string;
  statut?: StatutPaiement;
  modePaiement?: ModePaiement;
  reference?: string;
}

export interface TarifTicket {
  id?: number;
  libelle: string;
  prix: number;
  dureeValiditeHeures: number;
  actif?: boolean;
}

export type StatutTicket = 'VALIDE' | 'UTILISE' | 'EXPIRE' | 'ANNULE';

export interface TicketEntree {
  id?: number;
  numero?: string;
  qrCodeToken?: string;
  site?: Site;
  prix: number;
  modePaiement: ModePaiement;
  datePaiement?: string;
  dateValidite?: string;
  statut?: StatutTicket;
  nomClient?: string;
  telephoneClient?: string;
  tarif?: TarifTicket;
}

export interface TicketVenteResponse {
  id: number;
  numero: string;
  qrCodeToken: string;
  qrCodeBase64: string;
  siteId: number;
  siteNom: string;
  prix: number;
  modePaiement: string;
  dateValidite: string;
  nomClient?: string;
  telephoneClient: string;   // format international sans « + » (ex. 221771234567)
  lienTicket: string;        // page publique /t/{token} avec le QR
  lienWhatsApp: string;      // lien wa.me avec message pré-rempli
}

export interface ScanResponse {
  autorise: boolean;
  type?: 'MEMBRE' | 'TICKET';
  motifRefus?: string;
  nom?: string;
  prenom?: string;
  numero?: string;        // numeroCarte ou numeroTicket
  siteNom?: string;
  heureEntree?: string;   // ISO instant
  planNom?: string;       // pour les membres
  dateValidite?: string;  // pour les tickets
}

export interface PointageMembre {
  id: number;
  heureEntree: string;
  heureSortie?: string;
  statut: 'AUTORISE' | 'REFUSE';
  motifRefus?: string;
  typeAcces?: 'MEMBRE' | 'TICKET';
  scanSource?: 'TABLETTE' | 'MANUEL' | 'APP';
  siteNom?: string;
}

export interface DashboardStats {
  siteId?: number;
  siteNom?: string;
  membresActifs: number;
  membresEnRetard: number;
  membresSuspendus: number;
  pointagesAujourdhui: number;
  ticketsVendusAujourdhui: number;
  caJournalier: number;
  caMensualitesMois: number;
  caTicketsMois: number;
  caTotalMois: number;
  membresPayesCeMois: number;
  tauxRecouvrement: number;
}

export interface AppUser {
  id?: number;
  username: string;
  email?: string;
  nom?: string;
  prenom?: string;
  roles?: string[];
  actif?: boolean;
}

// ── Gérants ──────────────────────────────────────────────────────────────────

export interface GerantSiteAssignment {
  id: number;
  siteId: number;
  siteNom: string;
  heureDebut: string;   // "HH:mm:ss" depuis le backend
  heureFin: string;
  actif: boolean;
}

export interface CurrentUser {
  id: number;
  username: string;
  email?: string;
  nom?: string;
  prenom?: string;
  roles: string[];
  siteAssignments: GerantSiteAssignment[];
}

export interface Gerant {
  id?: number;
  userId?: number;
  username: string;
  email?: string;
  nom: string;
  prenom: string;
  siteId: number;
  siteNom?: string;
  heureDebut: string;
  heureFin: string;
  actif?: boolean;
}

export interface GerantStats {
  gerantId: number;
  nom: string;
  prenom: string;
  username: string;
  siteId: number;
  siteNom: string;
  heureDebut: string;
  heureFin: string;
  nbMensualitesEnregistrees: number;
  caMensualites: number;
  nbTicketsVendus: number;
  caTickets: number;
  caTotal: number;
  membresActifsSite: number;
  membresPayesCeMois: number;
  tauxRecouvrement: number;
}
