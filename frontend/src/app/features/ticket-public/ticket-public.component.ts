import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { environment } from '../../../environments/environment';

interface TicketPublic {
  numero: string;
  siteNom: string;
  tarif: string;
  prix: number;
  dateValidite: string;
  statut: 'VALIDE' | 'UTILISE' | 'EXPIRE' | 'ANNULE';
  nomClient?: string;
}

/**
 * Page ouverte par le client depuis le lien WhatsApp : affiche le QR en grand
 * pour qu'il le présente au scanner de l'accueil. Aucune connexion requise.
 */
@Component({
  selector: 'app-ticket-public',
  templateUrl: './ticket-public.component.html',
  styleUrls: ['./ticket-public.component.scss']
})
export class TicketPublicComponent implements OnInit {
  ticket?: TicketPublic;
  qrUrl = '';
  introuvable = false;

  constructor(private route: ActivatedRoute, private http: HttpClient) {}

  ngOnInit() {
    const token = this.route.snapshot.paramMap.get('token') ?? '';
    const base = `${environment.apiUrl}/api/public/tickets/${encodeURIComponent(token)}`;
    this.qrUrl = `${base}/qrcode`;
    this.http.get<TicketPublic>(base).subscribe({
      next: t => this.ticket = t,
      error: () => this.introuvable = true
    });
  }

  get expire(): boolean {
    return !!this.ticket && (this.ticket.statut === 'EXPIRE'
      || new Date(this.ticket.dateValidite).getTime() < Date.now());
  }

  get utilisable(): boolean {
    return !!this.ticket && this.ticket.statut === 'VALIDE' && !this.expire;
  }
}
