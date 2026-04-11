import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { MembreService } from '../../../core/services/membre.service';
import { MensualiteService } from '../../../core/services/mensualite.service';
import { Membre, Mensualite } from '../../../core/models/models';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-membre-detail',
  templateUrl: './membre-detail.component.html',
  styleUrls: ['./membre-detail.component.scss']
})
export class MembreDetailComponent implements OnInit {
  membre?: Membre;
  mensualites: Mensualite[] = [];
  qrUrl?: SafeUrl;
  loading = false;

  constructor(private route: ActivatedRoute,
              private membreSvc: MembreService,
              private mensSvc: MensualiteService,
              private sanitizer: DomSanitizer) {}

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.loading = true;
    this.membreSvc.get(id).subscribe(m => {
      this.membre = m;
      this.qrUrl = this.sanitizer.bypassSecurityTrustUrl(
        `${environment.apiUrl}/api/membres/${id}/qrcode`
      );
      this.loading = false;
    });
    this.mensSvc.parMembre(id).subscribe(list => this.mensualites = list);
  }

  imprimerCarte() { window.print(); }
}
