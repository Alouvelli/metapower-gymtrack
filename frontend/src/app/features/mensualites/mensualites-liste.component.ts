import { Component, OnInit, ViewChild, AfterViewInit } from '@angular/core';
import { MatPaginator } from '@angular/material/paginator';
import { MatTableDataSource } from '@angular/material/table';
import { FormBuilder, Validators } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MensualiteService } from '../../core/services/mensualite.service';
import { MembreService } from '../../core/services/membre.service';
import { Membre, Mensualite } from '../../core/models/models';

@Component({
  selector: 'app-mensualites-liste',
  templateUrl: './mensualites-liste.component.html',
  styleUrls: ['./mensualites-liste.component.scss']
})
export class MensualitesListeComponent implements OnInit, AfterViewInit {

  displayed = ['membre', 'mois', 'montant', 'date', 'mode', 'statut'];
  ds = new MatTableDataSource<Mensualite>([]);
  membres: Membre[] = [];
  showForm = false;

  @ViewChild(MatPaginator) paginator!: MatPaginator;

  form = this.fb.group({
    membreId: [null as number | null, Validators.required],
    moisConcerne: ['', Validators.required],
    montant: [0, Validators.required],
    modePaiement: ['ESPECES', Validators.required],
    reference: ['']
  });

  constructor(private fb: FormBuilder,
              private mensSvc: MensualiteService,
              private membreSvc: MembreService,
              private snack: MatSnackBar) {}

  ngOnInit() {
    this.charger();
    this.membreSvc.liste().subscribe(m => {
      this.membres = m;
    });
    // Auto-remplir le montant depuis le plan du membre sélectionné
    this.form.get('membreId')!.valueChanges.subscribe(id => {
      const membre = this.membres.find(m => m.id === id);
      if (membre?.planPrix) {
        this.form.patchValue({ montant: membre.planPrix });
      }
    });
  }

  ngAfterViewInit() { this.ds.paginator = this.paginator; }

  charger() {
    this.mensSvc.liste().subscribe(d => this.ds.data = d);
  }

  enregistrer() {
    if (this.form.invalid) return;
    const v = this.form.value;
    const payload = {
      membreId: v.membreId,
      moisConcerne: v.moisConcerne + '-01',
      montant: v.montant,
      modePaiement: v.modePaiement,
      reference: v.reference
    };
    this.mensSvc.enregistrerPaiement(payload).subscribe({
      next: () => {
        this.snack.open('Paiement enregistré', 'OK', { duration: 2000 });
        this.showForm = false;
        this.form.reset({ modePaiement: 'ESPECES', montant: 0 });
        this.charger();
      },
      error: () => this.snack.open('Erreur d\'enregistrement', 'OK', { duration: 3000 })
    });
  }
}
