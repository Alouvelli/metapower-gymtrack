import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },

  // Auth (publique)
  {
    path: 'login',
    loadChildren: () => import('./features/auth/login/login.module').then(m => m.LoginModule)
  },

  // Scanner tablette plein écran (route publique → utilise X-Device-Key)
  {
    path: 'scanner',
    loadChildren: () =>
      import('./features/pointages/scanner/scanner.module').then(m => m.ScannerModule)
  },

  // Modules protégés
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadChildren: () => import('./features/dashboard/dashboard.module').then(m => m.DashboardModule)
  },
  {
    path: 'membres',
    canActivate: [authGuard],
    loadChildren: () => import('./features/membres/membres.module').then(m => m.MembresModule)
  },
  {
    path: 'sites',
    canActivate: [authGuard],
    loadChildren: () => import('./features/sites/sites.module').then(m => m.SitesModule)
  },
  {
    path: 'mensualites',
    canActivate: [authGuard],
    loadChildren: () => import('./features/mensualites/mensualites.module').then(m => m.MensualitesModule)
  },
  {
    path: 'tickets',
    canActivate: [authGuard],
    loadChildren: () => import('./features/tickets/tickets.module').then(m => m.TicketsModule)
  },
  {
    path: 'pointages',
    canActivate: [authGuard],
    loadChildren: () => import('./features/pointages/pointages.module').then(m => m.PointagesModule)
  },
  {
    path: 'rapports',
    canActivate: [authGuard],
    loadChildren: () => import('./features/rapports/rapports.module').then(m => m.RapportsModule)
  },
  {
    path: 'parametres',
    canActivate: [authGuard],
    loadChildren: () => import('./features/parametres/parametres.module').then(m => m.ParametresModule)
  },

  { path: '**', redirectTo: 'dashboard' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {}
