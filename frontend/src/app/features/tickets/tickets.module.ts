import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { TicketsVenteComponent } from './vente/tickets-vente.component';
import { TicketsListeComponent } from './liste/tickets-liste.component';
import { TarifsComponent } from './tarifs/tarifs.component';

@NgModule({
  declarations: [TicketsVenteComponent, TicketsListeComponent, TarifsComponent],
  imports: [
    SharedModule,
    RouterModule.forChild([
      { path: '', redirectTo: 'vente', pathMatch: 'full' },
      { path: 'vente', component: TicketsVenteComponent },
      { path: 'liste', component: TicketsListeComponent },
      { path: 'tarifs', component: TarifsComponent }
    ])
  ]
})
export class TicketsModule {}
