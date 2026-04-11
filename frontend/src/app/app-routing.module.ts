import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { LayoutComponent } from './shared/layout/layout.component';

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

  // Modules protégés sous le shell layout
  {
    path: '',
    component: LayoutComponent,
    canActivate: [authGuard],
    children: [
      {
        path: 'dashboard',
        loadChildren: () => import('./features/dashboard/dashboard.module').then(m => m.DashboardModule)
      },
      {
        path: 'membres',
        loadChildren: () => import('./features/membres/membres.module').then(m => m.MembresModule)
      },
      {
        path: 'sites',
        loadChildren: () => import('./features/sites/sites.module').then(m => m.SitesModule)
      },
      {
        path: 'mensualites',
        loadChildren: () => import('./features/mensualites/mensualites.module').then(m => m.MensualitesModule)
      },
      {
        path: 'tickets',
        loadChildren: () => import('./features/tickets/tickets.module').then(m => m.TicketsModule)
      },
      {
        path: 'pointages',
        loadChildren: () => import('./features/pointages/pointages.module').then(m => m.PointagesModule)
      },
      {
        path: 'rapports',
        loadChildren: () => import('./features/rapports/rapports.module').then(m => m.RapportsModule)
      },
      {
        path: 'parametres',
        loadChildren: () => import('./features/parametres/parametres.module').then(m => m.ParametresModule)
      }
    ]
  },

  { path: '**', redirectTo: 'dashboard' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {}
