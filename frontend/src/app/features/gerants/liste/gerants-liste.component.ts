import { Component, OnInit, ViewChild } from '@angular/core';
import { MatTableDataSource } from '@angular/material/table';
import { MatSnackBar } from '@angular/material/snack-bar';
import { GerantService } from '../../../core/services/gerant.service';
import { SiteService } from '../../../core/services/site.service';
import { Gerant, Site } from '../../../core/models/models';
import { GerantFormComponent } from '../form/gerant-form.component';

@Component({
  selector: 'app-gerants-liste',
  templateUrl: './gerants-liste.component.html',
  styleUrls: ['./gerants-liste.component.scss']
})
export class GerantsListeComponent implements OnInit {

  @ViewChild(GerantFormComponent) formComp!: GerantFormComponent;

  sites: Site[] = [];
  selectedSiteId: number | null = null;

  ds = new MatTableDataSource<Gerant>([]);
  displayed = ['nom', 'username', 'site', 'creneau', 'actif', 'actions'];

  showForm = false;
  loading  = false;

  constructor(
    private gerantSvc: GerantService,
    private siteSvc:   SiteService,
    private snack:     MatSnackBar
  ) {}

  ngOnInit(): void {
    this.siteSvc.liste().subscribe(s => this.sites = s);
    this.charger();
  }

  charger(): void {
    this.loading = true;
    const obs$ = this.selectedSiteId
      ? this.gerantSvc.listeBySite(this.selectedSiteId)
      : this.gerantSvc.liste();
    obs$.subscribe({
      next: g => { this.ds.data = g; this.loading = false; },
      error: () => this.loading = false
    });
  }

  ouvrirFormulaire(): void { this.showForm = true; }

  onFormSaved(): void {
    this.showForm = false;
    this.charger();
    this.snack.open('Gérant créé avec succès', 'OK', { duration: 3000, panelClass: 'snack-success' });
  }

  onFormCancelled(): void { this.showForm = false; }

  desactiver(g: Gerant): void {
    if (!confirm(`Désactiver le compte de ${g.prenom} ${g.nom} ?`)) return;
    this.gerantSvc.desactiver(g.id!).subscribe(() => {
      this.snack.open('Gérant désactivé', 'OK', { duration: 3000 });
      this.charger();
    });
  }

  /** Formattage du créneau HH:mm:ss → HH:mm */
  creneau(g: Gerant): string {
    return `${this.gerantSvc.toHHmm(g.heureDebut)} – ${this.gerantSvc.toHHmm(g.heureFin)}`;
  }

  get totalActifs(): number { return this.ds.data.filter(g => g.actif).length; }
}
