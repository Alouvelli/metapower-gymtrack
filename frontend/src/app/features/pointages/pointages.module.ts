import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { HistoriqueComponent } from './historique/historique.component';

@NgModule({
  declarations: [HistoriqueComponent],
  imports: [
    SharedModule,
    RouterModule.forChild([
      { path: '', component: HistoriqueComponent }
    ])
  ]
})
export class PointagesModule {}
