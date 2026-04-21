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

export type StatutMembre = 'ACTIF' | 'EXPIRE' | 'SUSPENDU';

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
}

export interface ScanResponse {
  autorise: boolean;
  type?: 'MEMBRE' | 'TICKET';
  motif?: string;
  membre?: Membre;
  ticket?: TicketEntree;
  site?: Site;
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
  role?: string;
  actif?: boolean;
}
