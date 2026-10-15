import { Component, Inject, OnInit } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { GerantService } from '../../../core/services/gerant.service';
import { SiteService } from '../../../core/services/site.service';
import { Gerant, Site } from '../../../core/models/models';

/**
 * Création d'un gérant (tous les champs) ou modification de son créneau
 * horaire (seul champ modifiable côté API : PUT /api/gerants/{id}/horaire).
 */
@Component({
  selector: 'app-gerant-form',
  templateUrl: './gerant-form.component.html',
  styleUrls: ['./gerant-form.component.scss']
})
export class GerantFormComponent implements OnInit {
  sites: Site[] = [];
  enregistrement = false;
  erreur = '';

  form = this.fb.group({
    prenom:     ['', Validators.required],
    nom:        ['', Validators.required],
    email:      ['', Validators.email],
    username:   ['', Validators.required],
    password:   ['', [Validators.required, Validators.minLength(8)]],
    siteId:     [null as number | null, Validators.required],
    heureDebut: ['', Validators.required],
    heureFin:   ['', Validators.required]
  });

  constructor(
    private fb: FormBuilder,
    private gerantSvc: GerantService,
    private siteSvc: SiteService,
    private dialogRef: MatDialogRef<GerantFormComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { gerant?: Gerant }
  ) {}

  get edition(): boolean { return !!this.data?.gerant?.id; }

  ngOnInit() {
    if (this.edition) {
      const g = this.data.gerant!;
      this.form.patchValue({
        heureDebut: this.gerantSvc.toHHmm(g.heureDebut),
        heureFin:   this.gerantSvc.toHHmm(g.heureFin)
      });
      // En édition, seul le créneau est modifiable
      ['prenom', 'nom', 'email', 'username', 'password', 'siteId']
        .forEach(c => this.form.get(c)!.disable());
    } else {
      this.siteSvc.liste().subscribe(s => this.sites = s.filter(site => site.actif !== false));
    }
  }

  enregistrer() {
    if (this.form.invalid) return;
    const v = this.form.getRawValue();
    if (v.heureDebut! >= v.heureFin!) {
      this.erreur = "L'heure de fin doit être après l'heure de début.";
      return;
    }
    this.erreur = '';
    this.enregistrement = true;

    const obs = this.edition
      ? this.gerantSvc.updateHoraire(this.data.gerant!.id!, v.heureDebut!, v.heureFin!)
      : this.gerantSvc.creer({
          prenom: v.prenom!, nom: v.nom!, email: v.email || undefined,
          username: v.username!, password: v.password!, siteId: v.siteId!,
          heureDebut: v.heureDebut!, heureFin: v.heureFin!
        });

    obs.subscribe({
      next: () => this.dialogRef.close(true),
      error: err => {
        this.enregistrement = false;
        this.erreur = err?.error?.message || "Erreur lors de l'enregistrement.";
      }
    });
  }
}
