import { Component, OnInit } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MembreService } from '../../../core/services/membre.service';
import { PlanService } from '../../../core/services/plan.service';
import { PlanAbonnement } from '../../../core/models/models';

@Component({
  selector: 'app-membre-form',
  templateUrl: './membre-form.component.html',
  styleUrls: ['./membre-form.component.scss']
})
export class MembreFormComponent implements OnInit {
  id: number | null = null;
  plans: PlanAbonnement[] = [];
  loading = false;

  form = this.fb.group({
    nom: ['', Validators.required],
    prenom: ['', Validators.required],
    telephone: ['', Validators.required],
    email: [''],
    dateNaissance: [''],
    planId: [null as number | null]
  });

  constructor(private fb: FormBuilder,
              private route: ActivatedRoute,
              private router: Router,
              private membreSvc: MembreService,
              private planSvc: PlanService,
              private snack: MatSnackBar) {}

  ngOnInit() {
    this.planSvc.liste().subscribe(p => this.plans = p);
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.id = Number(idParam);
      this.membreSvc.get(this.id).subscribe(m => {
        this.form.patchValue({
          nom: m.nom, prenom: m.prenom, telephone: m.telephone,
          email: m.email || '', dateNaissance: m.dateNaissance || '',
          planId: m.planId ?? null
        });
      });
    }
  }

  submit() {
    if (this.form.invalid) return;
    this.loading = true;
    const payload: any = this.form.value;
    const obs = this.id
      ? this.membreSvc.modifier(this.id, payload)
      : this.membreSvc.creer(payload);
    obs.subscribe({
      next: m => {
        this.snack.open('Membre enregistré', 'OK', { duration: 2500 });
        this.router.navigate(['/membres', m.id]);
      },
      error: () => {
        this.loading = false;
        this.snack.open('Erreur d\'enregistrement', 'OK', { duration: 3000 });
      }
    });
  }
}
