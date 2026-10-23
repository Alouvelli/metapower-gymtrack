import { Component, OnInit, ViewChild, AfterViewInit } from '@angular/core';
import { MatPaginator } from '@angular/material/paginator';
import { MatTableDataSource } from '@angular/material/table';
import { FormBuilder, Validators } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MensualiteService } from '../../core/services/mensualite.service';
import { MembreService } from '../../core/services/membre.service';
import { Membre, Mensualite } from '../../core/models/models';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-mensualites-liste',
  templateUrl: './mensualites-liste.component.html',
  styleUrls: ['./mensualites-liste.component.scss']
})
export class MensualitesListeComponent implements OnInit, AfterViewInit {

  displayed = ['membre', 'mois', 'montant', 'date', 'mode', 'statut', 'actions'];
  enregistrement = false;
  ds = new MatTableDataSource<Mensualite>([]);
  membres: Membre[] = [];
  showForm = false;

  @ViewChild(MatPaginator) paginator!: MatPaginator;

  form = this.fb.group({
    membreId: [null as number | null, Validators.required],
    dateDebut: [''],   // vide = calculé par le serveur (paiement ou fin d'abonnement en cours)
    montant: [0, Validators.required],
    modePaiement: ['ESPECES', Validators.required],
    reference: ['']
  });

  constructor(private fb: FormBuilder,
              private mensSvc: MensualiteService,
              private membreSvc: MembreService,
              private snack: MatSnackBar,
              public auth: AuthService) {}

  /** Annulation d'un paiement saisi par erreur — admin uniquement (contrôlé aussi côté serveur). */
  annuler(m: Mensualite) {
    if (!m.id) return;
    const qui = m.membre ? `${m.membre.prenom} ${m.membre.nom}` : 'ce membre';
    const motif = window.prompt(`Annuler le paiement de ${m.montant} FCFA de ${qui} (${m.datePaiement}) ?\n\nMotif de l'annulation (obligatoire) :`, 'Doublon');
    if (motif === null) return;
    if (motif.trim().length < 3) { this.snack.open('Motif obligatoire (3 caractères minimum)', 'OK', { duration: 3000 }); return; }
    this.mensSvc.annuler(m.id, motif.trim()).subscribe({
      next: () => { this.snack.open('Paiement annulé', 'OK', { duration: 2500 }); this.charger(); },
      error: err => this.snack.open(err?.error?.message || 'Annulation impossible', 'OK', { duration: 4000 })
    });
  }

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

  get totalPaye():     number { return this.ds.data.filter(m => m.statut === 'PAYE').length; }
  get totalEnAttente(): number { return this.ds.data.filter(m => m.statut === 'EN_ATTENTE').length; }

  charger() {
    this.mensSvc.liste().subscribe(d => this.ds.data = d);
  }

  enregistrer() {
    // Un double clic envoyait deux paiements identiques : on bloque pendant l'envoi
    if (this.form.invalid || this.enregistrement) return;
    this.enregistrement = true;
    const v = this.form.value;
    const payload = {
      membreId: v.membreId,
      dateDebut: v.dateDebut || null,
      montant: v.montant,
      modePaiement: v.modePaiement,
      reference: v.reference
    };
    this.mensSvc.enregistrerPaiement(payload).subscribe({
      next: () => {
        this.enregistrement = false;
        this.snack.open('Paiement enregistré', 'OK', { duration: 2000 });
        this.showForm = false;
        this.form.reset({ modePaiement: 'ESPECES', montant: 0 });
        this.charger();
      },
      error: err => {
        this.enregistrement = false;
        this.snack.open(err?.error?.message || 'Erreur d\'enregistrement', 'OK', { duration: 4000 });
      }
    });
  }
}
