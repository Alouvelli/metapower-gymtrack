import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { GerantService } from '../../../core/services/gerant.service';
import { Site } from '../../../core/models/models';

@Component({
  selector: 'app-gerant-form',
  templateUrl: './gerant-form.component.html',
  styleUrls: ['./gerant-form.component.scss']
})
export class GerantFormComponent implements OnInit {

  @Input()  sites: Site[] = [];
  @Output() saved     = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();

  form!: FormGroup;
  loading      = false;
  showPassword = false;
  errorMsg     = '';

  constructor(
    private fb:         FormBuilder,
    private gerantSvc:  GerantService
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      nom:        ['', Validators.required],
      prenom:     ['', Validators.required],
      username:   ['', Validators.required],
      password:   ['', [Validators.required, Validators.minLength(6)]],
      email:      ['', Validators.email],
      siteId:     [null, Validators.required],
      heureDebut: ['', Validators.required],
      heureFin:   ['', Validators.required],
    });
  }

  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.loading  = true;
    this.errorMsg = '';
    const v = this.form.value;
    this.gerantSvc.creer({
      nom:        v.nom,
      prenom:     v.prenom,
      username:   v.username,
      password:   v.password,
      email:      v.email || undefined,
      siteId:     Number(v.siteId),
      heureDebut: v.heureDebut,
      heureFin:   v.heureFin,
    }).subscribe({
      next: () => { this.loading = false; this.saved.emit(); },
      error: err => {
        this.loading  = false;
        this.errorMsg = err?.error?.message ?? 'Erreur lors de la création';
      }
    });
  }

  cancel(): void { this.cancelled.emit(); }
}
