import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { MembreService } from '../../../core/services/membre.service';
import { MensualiteService } from '../../../core/services/mensualite.service';
import { PointageService } from '../../../core/services/pointage.service';
import { Membre, Mensualite, PointageMembre } from '../../../core/models/models';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-membre-detail',
  templateUrl: './membre-detail.component.html',
  styleUrls: ['./membre-detail.component.scss']
})
export class MembreDetailComponent implements OnInit {
  membre?: Membre;
  mensualites: Mensualite[] = [];
  pointages: PointageMembre[] = [];
  qrUrl?: SafeUrl;
  loading = false;
  activeTab: 'infos' | 'carte' | 'mensualites' | 'pointages' = 'infos';

  constructor(private route: ActivatedRoute,
              private membreSvc: MembreService,
              private mensSvc: MensualiteService,
              private pointageSvc: PointageService,
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
    this.pointageSvc.parMembre(id).subscribe(list => this.pointages = list);
  }

  /**
   * Impression de la carte : la mise en page vient uniquement du CSS
   * « @media print » activé par body.printing-carte (styles.scss).
   * Aucun style n'est posé en JS sur le layout : avant, les styles inline
   * (position fixed, marges à 0) restaient après l'impression et le contenu
   * passait sous le menu, car « afterprint » était écouté APRÈS window.print()
   * — or Chrome déclenche afterprint avant que window.print() ne rende la main.
   */
  imprimerCarte() {
    const body = document.body;
    const terminer = () => body.classList.remove('printing-carte');
    window.addEventListener('afterprint', terminer, { once: true });   // avant print()
    body.classList.add('printing-carte');
    window.print();
  }
}
