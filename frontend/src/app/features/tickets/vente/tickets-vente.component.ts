import { Component, OnInit } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import { TicketService } from '../../../core/services/ticket.service';
import { SiteService } from '../../../core/services/site.service';
import { Site, TarifTicket, TicketVenteResponse } from '../../../core/models/models';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-tickets-vente',
  templateUrl: './tickets-vente.component.html',
  styleUrls: ['./tickets-vente.component.scss']
})
export class TicketsVenteComponent implements OnInit {

  tarifs: TarifTicket[] = [];
  sites: Site[] = [];
  dernier?: TicketVenteResponse;
  loading = false;

  form = this.fb.group({
    tarifId: [null as number | null, Validators.required],
    siteId: [null as number | null, Validators.required],
    modePaiement: ['ESPECES', Validators.required],
    nomClient: [''],
    telephoneClient: ['']
  });

  constructor(private fb: FormBuilder,
              private ticketSvc: TicketService,
              private siteSvc: SiteService,
              private snack: MatSnackBar) {}

  ngOnInit() {
    this.ticketSvc.listeTarifs().subscribe(t => this.tarifs = t.filter(x => x.actif !== false));
    this.siteSvc.liste().subscribe(s => {
      this.sites = s;
      const stored = localStorage.getItem('site-id');
      if (stored) this.form.patchValue({ siteId: Number(stored) });
    });
  }

  prixSelectionne(): number {
    const id = this.form.value.tarifId;
    return this.tarifs.find(t => t.id === id)?.prix ?? 0;
  }

  vendre() {
    if (this.form.invalid) return;
    this.loading = true;
    if (this.form.value.siteId) localStorage.setItem('site-id', String(this.form.value.siteId));
    this.ticketSvc.vendre(this.form.value).subscribe({
      next: r => {
        this.dernier = r;
        this.loading = false;
        this.snack.open('Ticket vendu — N° ' + r.numero, 'OK', { duration: 3000 });
        this.form.patchValue({ nomClient: '', telephoneClient: '' });
      },
      error: () => {
        this.loading = false;
        this.snack.open('Erreur lors de la vente', 'OK', { duration: 3000 });
      }
    });
  }

  imprimerRecu() {
    if (!this.dernier?.id) return;
    window.open(`${environment.apiUrl}/api/tickets/${this.dernier.id}/recu`, '_blank');
  }
}
