import { Component, OnInit } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SiteService } from '../../core/services/site.service';
import { Site } from '../../core/models/models';

@Component({
  selector: 'app-sites',
  templateUrl: './sites.component.html',
  styleUrls: ['./sites.component.scss']
})
export class SitesComponent implements OnInit {
  sites: Site[] = [];
  edit: Site | null = null;

  form = this.fb.group({
    nom: ['', Validators.required],
    adresse: [''],
    telephone: [''],
    email: [''],
    actif: [true]
  });

  constructor(private fb: FormBuilder, private siteSvc: SiteService, private snack: MatSnackBar) {}

  ngOnInit() { this.charger(); }

  get sitesActifs():   number { return this.sites.filter(s => s.actif).length; }
  get sitesInactifs(): number { return this.sites.filter(s => !s.actif).length; }

  charger() { this.siteSvc.liste().subscribe(s => this.sites = s); }

  nouveau() {
    this.edit = {} as Site;
    this.form.reset({ actif: true });
  }

  modifier(s: Site) {
    this.edit = s;
    this.form.patchValue(s);
  }

  annuler() { this.edit = null; }

  enregistrer() {
    if (this.form.invalid) return;
    const payload: any = this.form.value;
    const obs = this.edit?.id
      ? this.siteSvc.modifier(this.edit.id, payload)
      : this.siteSvc.creer(payload);
    obs.subscribe(() => {
      this.snack.open('Site enregistré', 'OK', { duration: 2000 });
      this.edit = null;
      this.charger();
    });
  }

  supprimer(s: Site) {
    if (!s.id || !confirm(`Supprimer la salle "${s.nom}" ?`)) return;
    this.siteSvc.supprimer(s.id).subscribe(() => this.charger());
  }
}
