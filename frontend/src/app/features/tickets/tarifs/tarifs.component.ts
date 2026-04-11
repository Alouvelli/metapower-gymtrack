import { Component, OnInit } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import { TicketService } from '../../../core/services/ticket.service';
import { TarifTicket } from '../../../core/models/models';

@Component({
  selector: 'app-tarifs',
  templateUrl: './tarifs.component.html',
  styleUrls: ['./tarifs.component.scss']
})
export class TarifsComponent implements OnInit {

  tarifs: TarifTicket[] = [];
  edit: TarifTicket | null = null;

  form = this.fb.group({
    libelle: ['', Validators.required],
    prix: [0, Validators.required],
    dureeValiditeHeures: [24, Validators.required],
    actif: [true]
  });

  constructor(private fb: FormBuilder, private ticketSvc: TicketService, private snack: MatSnackBar) {}

  ngOnInit() { this.charger(); }

  charger() { this.ticketSvc.listeTarifs().subscribe(t => this.tarifs = t); }

  nouveau() {
    this.edit = {} as TarifTicket;
    this.form.reset({ actif: true, dureeValiditeHeures: 24, prix: 0 });
  }

  modifier(t: TarifTicket) {
    this.edit = t;
    this.form.patchValue(t);
  }

  enregistrer() {
    if (this.form.invalid) return;
    const payload: any = this.form.value;
    const obs = this.edit?.id
      ? this.ticketSvc.modifierTarif(this.edit.id, payload)
      : this.ticketSvc.creerTarif(payload);
    obs.subscribe(() => {
      this.snack.open('Tarif enregistré', 'OK', { duration: 2000 });
      this.edit = null;
      this.charger();
    });
  }

  supprimer(t: TarifTicket) {
    if (!t.id || !confirm(`Supprimer le tarif "${t.libelle}" ?`)) return;
    this.ticketSvc.supprimerTarif(t.id).subscribe(() => this.charger());
  }
}
