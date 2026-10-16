import { Component, OnDestroy, OnInit } from '@angular/core';
import { interval, Subscription } from 'rxjs';
import { PointageService } from '../../../core/services/pointage.service';
import { SiteService } from '../../../core/services/site.service';
import { Site } from '../../../core/models/models';

@Component({
  selector: 'app-historique',
  templateUrl: './historique.component.html',
  styleUrls: ['./historique.component.scss']
})
export class HistoriqueComponent implements OnInit, OnDestroy {

  sites: Site[] = [];
  siteId: number | null = null;
  pointages: any[] = [];
  displayed = ['heure', 'type', 'identite', 'statut', 'motif'];
  private sub?: Subscription;

  constructor(private pointageSvc: PointageService, private siteSvc: SiteService) {}

  ngOnInit() {
    this.siteSvc.liste().subscribe(s => {
      this.sites = s;
      if (s.length) { this.siteId = s[0].id!; this.charger(); }
    });
    // Auto-refresh toutes les 10 secondes
    this.sub = interval(10000).subscribe(() => this.charger());
  }

  ngOnDestroy() { this.sub?.unsubscribe(); }

  get pointagesAutorises(): number { return this.pointages.filter(p => p.statut === 'AUTORISE').length; }
  get pointagesRefuses():   number { return this.pointages.filter(p => p.statut !== 'AUTORISE').length; }

  changerSite() { this.charger(); }

  charger() {
    if (!this.siteId) return;
    this.pointageSvc.jour(this.siteId).subscribe(p => this.pointages = p);
  }
}
