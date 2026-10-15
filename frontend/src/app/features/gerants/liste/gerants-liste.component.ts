import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { GerantService } from '../../../core/services/gerant.service';
import { Gerant } from '../../../core/models/models';
import { GerantFormComponent } from '../form/gerant-form.component';

@Component({
  selector: 'app-gerants-liste',
  templateUrl: './gerants-liste.component.html',
  styleUrls: ['./gerants-liste.component.scss']
})
export class GerantsListeComponent implements OnInit {
  gerants: Gerant[] = [];
  colonnes = ['nom', 'username', 'site', 'creneau', 'statut', 'actions'];

  constructor(public gerantSvc: GerantService, private dialog: MatDialog, private snack: MatSnackBar) {}

  ngOnInit() { this.charger(); }

  charger() { this.gerantSvc.liste().subscribe(g => this.gerants = g); }

  nouveau() { this.ouvrir(); }

  modifierHoraire(g: Gerant) { this.ouvrir(g); }

  private ouvrir(gerant?: Gerant) {
    this.dialog.open(GerantFormComponent, { width: '520px', data: { gerant } })
      .afterClosed()
      .subscribe(ok => {
        if (!ok) return;
        this.snack.open('Gérant enregistré', 'OK', { duration: 2000 });
        this.charger();
      });
  }

  desactiver(g: Gerant) {
    if (!g.id || !confirm(`Désactiver le gérant ${g.prenom} ${g.nom} ?`)) return;
    this.gerantSvc.desactiver(g.id).subscribe(() => {
      this.snack.open('Gérant désactivé', 'OK', { duration: 2000 });
      this.charger();
    });
  }
}
