import { Component, OnInit } from '@angular/core';
import { RapportService } from '../../core/services/rapport.service';
import { SiteService } from '../../core/services/site.service';
import { Site } from '../../core/models/models';

@Component({
  selector: 'app-rapports',
  templateUrl: './rapports.component.html',
  styleUrls: ['./rapports.component.scss']
})
export class RapportsComponent implements OnInit {

  mois = this.moisCourant();
  sites: Site[] = [];
  siteId: number | null = null;
  loading = false;

  constructor(private rapportSvc: RapportService, private siteSvc: SiteService) {}

  ngOnInit() {
    this.siteSvc.liste().subscribe(s => this.sites = s);
  }

  private moisCourant(): string {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
  }

  private moisIso(): string { return this.mois + '-01'; }

  exportMensXls() {
    this.loading = true;
    this.rapportSvc.mensualitesExcel(this.moisIso()).subscribe({
      next: b => { this.rapportSvc.download(b, `mensualites-${this.mois}.xlsx`); this.loading = false; },
      error: () => this.loading = false
    });
  }

  exportMensPdf() {
    this.loading = true;
    this.rapportSvc.mensualitesPdf(this.moisIso()).subscribe({
      next: b => { this.rapportSvc.download(b, `mensualites-${this.mois}.pdf`); this.loading = false; },
      error: () => this.loading = false
    });
  }

  exportTicketsXls() {
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
}
