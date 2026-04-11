import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

interface NavItem { route: string; icon: string; label: string; }

@Component({
  selector: 'app-layout',
  templateUrl: './layout.component.html',
  styleUrls: ['./layout.component.scss']
})
export class LayoutComponent {

  readonly nav: NavItem[] = [
    { route: '/dashboard',   icon: 'dashboard',     label: 'Tableau de bord' },
    { route: '/membres',     icon: 'group',         label: 'Membres' },
    { route: '/mensualites', icon: 'payments',      label: 'Mensualités' },
    { route: '/tickets',     icon: 'confirmation_number', label: 'Tickets' },
    { route: '/pointages',   icon: 'qr_code_scanner', label: 'Pointages' },
    { route: '/sites',       icon: 'store',         label: 'Salles' },
    { route: '/rapports',    icon: 'assessment',    label: 'Rapports' },
    { route: '/parametres',  icon: 'settings',      label: 'Paramètres' }
  ];

  constructor(private auth: AuthService, private router: Router) {}

  logout() {
    this.auth.logout();
    this.router.navigateByUrl('/login');
  }
}
