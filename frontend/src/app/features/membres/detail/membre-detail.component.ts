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

  imprimerCarte() {
    const html = document.documentElement;
    const prevHtmlStyle = html.getAttribute('style') || '';

    // ① Force le rendu des couleurs et arrière-plans dans tous les navigateurs
    html.style.setProperty('-webkit-print-color-adjust', 'exact');
    html.style.setProperty('print-color-adjust', 'exact');
    html.style.setProperty('color-adjust', 'exact');

    // ② Contourne le margin-left inline injecté par Angular Material sur mat-sidenav-content.
    //    Le CSS !important ne suffit pas toujours face aux styles inline lors du rendu d'impression.
    const sidenavContent = document.querySelector('mat-sidenav-content') as HTMLElement | null;
    const prevML  = sidenavContent?.style.getPropertyValue('margin-left')  ?? '';
    const prevMR  = sidenavContent?.style.getPropertyValue('margin-right') ?? '';
    const prevTr  = sidenavContent?.style.getPropertyValue('transform')    ?? '';
    const prevPos = sidenavContent?.style.getPropertyValue('position')     ?? '';

    if (sidenavContent) {
      sidenavContent.style.setProperty('margin-left',  '0',     'important');
      sidenavContent.style.setProperty('margin-right', '0',     'important');
      sidenavContent.style.setProperty('transform',    'none',  'important');
      sidenavContent.style.setProperty('position',     'fixed', 'important');
      sidenavContent.style.setProperty('top',          '0',     'important');
      sidenavContent.style.setProperty('left',         '0',     'important');
    }

    document.body.classList.add('printing-carte');
    window.print();

    // ③ Restaure l'état après impression
    window.addEventListener('afterprint', () => {
      document.body.classList.remove('printing-carte');
      html.setAttribute('style', prevHtmlStyle);
      if (sidenavContent) {
        prevML  ? sidenavContent.style.setProperty('margin-left',  prevML)  : sidenavContent.style.removeProperty('margin-left');
        prevMR  ? sidenavContent.style.setProperty('margin-right', prevMR)  : sidenavContent.style.removeProperty('margin-right');
        prevTr  ? sidenavContent.style.setProperty('transform',    prevTr)  : sidenavContent.style.removeProperty('transform');
        prevPos ? sidenavContent.style.setProperty('position',     prevPos) : sidenavContent.style.removeProperty('position');
        sidenavContent.style.removeProperty('top');
        sidenavContent.style.removeProperty('left');
      }
    }, { once: true });
  }
}
