import { Component, OnInit } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import { PlanService } from '../../core/services/plan.service';
import { PlanAbonnement } from '../../core/models/models';

@Component({
  selector: 'app-parametres',
  templateUrl: './parametres.component.html',
  styleUrls: ['./parametres.component.scss']
})
export class ParametresComponent implements OnInit {

  plans: PlanAbonnement[] = [];
  edit: PlanAbonnement | null = null;
  activeTab: 'plans' | 'securite' = 'plans';

  get plansActifs():   number { return this.plans.filter(p => p.actif !== false).length; }
  get plansInactifs(): number { return this.plans.filter(p => p.actif === false).length; }

  form = this.fb.group({
    nom: ['', Validators.required],
    dureeEnMois: [1, Validators.required],
    prix: [0, Validators.required],
    actif: [true]
  });

  constructor(private fb: FormBuilder, private planSvc: PlanService, private snack: MatSnackBar) {}

  ngOnInit() { this.charger(); }

  charger() { this.planSvc.liste().subscribe(p => this.plans = p); }

  nouveau() {
    this.edit = {} as PlanAbonnement;
    this.form.reset({ actif: true, dureeEnMois: 1, prix: 0 });
  }

  modifier(p: PlanAbonnement) {
    this.edit = p;
    this.form.patchValue(p);
  }

  enregistrer() {
    if (this.form.invalid) return;
    const payload: any = this.form.value;
    const obs = this.edit?.id
      ? this.planSvc.modifier(this.edit.id, payload)
      : this.planSvc.creer(payload);
    obs.subscribe(() => {
      this.snack.open('Plan enregistré', 'OK', { duration: 2000 });
      this.edit = null;
      this.charger();
    });
  }

  supprimer(p: PlanAbonnement) {
    if (!p.id || !confirm(`Supprimer le plan "${p.nom}" ?`)) return;
    this.planSvc.supprimer(p.id).subscribe(() => this.charger());
  }
}
