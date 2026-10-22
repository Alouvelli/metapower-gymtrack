import { Component, OnInit } from '@angular/core';
import { MatTableDataSource } from '@angular/material/table';
import { RapportService } from '../../core/services/rapport.service';
import { SiteService } from '../../core/services/site.service';
import { GerantService } from '../../core/services/gerant.service';
import { Site, GerantStats } from '../../core/models/models';

@Component({
  selector: 'app-rapports',
  templateUrl: './rapports.component.html',
  styleUrls: ['./rapports.component.scss']
})
export class RapportsComponent implements OnInit {

  mois    = this.moisCourant();
  sites:  Site[] = [];
  siteId: number | null = null;
  loading = false;

  // ── Gérant stats ────────────────────────────────────────────────────────────
  gerantDs = new MatTableDataSource<GerantStats>([]);
  gerantCols = ['gerant', 'site', 'creneau', 'mensualites', 'caMens', 'tickets', 'caTickets', 'caTotal', 'taux'];
  loadingGerant = false;

  constructor(
    private rapportSvc: RapportService,
    private siteSvc:    SiteService,
    private gerantSvc:  GerantService
  ) {}

  ngOnInit(): void {
    this.siteSvc.liste().subscribe(s => this.sites = s);
    this.chargerStatsGerant();
  }

  private moisCourant(): string {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
  }

  private moisIso(): string { return this.mois + '-01'; }

  // ── Stats par gérant ────────────────────────────────────────────────────────

  chargerStatsGerant(): void {
    this.loadingGerant = true;
    this.gerantSvc.statsParGerant(this.mois).subscribe({
      next:  s => { this.gerantDs.data = s; this.loadingGerant = false; },
      error: () => { this.loadingGerant = false; }
    });
  }

  exportGerantsCsv(): void {
    const header = ['Gérant', 'Username', 'Salle', 'Créneau',
      'Nb Mensualités', 'CA Mensualités (FCFA)',
      'Nb Tickets', 'CA Tickets (FCFA)', 'CA Total (FCFA)',
      'Membres actifs site', 'Taux recouvrement (%)'].join(';');

    const rows = this.gerantDs.data.map(g => [
      `${g.nom} ${g.prenom}`,
      g.username,
      g.siteNom,
      `${g.heureDebut.substring(0, 5)}–${g.heureFin.substring(0, 5)}`,
      g.nbMensualitesEnregistrees,
      g.caMensualites,
      g.nbTicketsVendus,
      g.caTickets,
      g.caTotal,
      g.membresActifsSite,
      g.tauxRecouvrement
    ].join(';'));

    // BOM ensures proper UTF-8 encoding in Excel
    const csv  = '﻿' + [header, ...rows].join('\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    this.rapportSvc.download(blob, `rapport-gerants-${this.mois}.csv`);
  }

  // ── Rapport exports ────────────────────────────────────────────────────────

  exportMensXls(): void {
    this.loading = true;
    this.rapportSvc.mensualitesExcel(this.moisIso()).subscribe({
      next: b => { this.rapportSvc.download(b, `mensualites-${this.mois}.xlsx`); this.loading = false; },
      error: () => this.loading = false
    });
  }

  exportMensPdf(): void {
    this.loading = true;
    this.rapportSvc.mensualitesPdf(this.moisIso()).subscribe({
      next: b => { this.rapportSvc.download(b, `mensualites-${this.mois}.pdf`); this.loading = false; },
      error: () => this.loading = false
    });
  }

  exportTicketsXls(): void {
    this.loading = true;
    this.rapportSvc.ticketsExcel(this.moisIso(), this.siteId ?? undefined).subscribe({
      next: b => {
        const suffix = this.siteId ? `-site${this.siteId}` : '';
        this.rapportSvc.download(b, `tickets-${this.mois}${suffix}.xlsx`);
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  // ── Formatting helpers ─────────────────────────────────────────────────────

  formatFcfa(n: number): string {
    if (n >= 1_000_000) return (n / 1_000_000).toFixed(1).replace('.0', '') + 'M';
    if (n >= 1_000)     return Math.round(n / 1_000) + 'k';
    return n.toLocaleString('fr-FR');
  }

  creneau(g: GerantStats): string {
    return `${g.heureDebut.substring(0, 5)} – ${g.heureFin.substring(0, 5)}`;
  }
}
