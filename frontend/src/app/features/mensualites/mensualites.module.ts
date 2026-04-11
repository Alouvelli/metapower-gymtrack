import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { MensualitesListeComponent } from './mensualites-liste.component';

@NgModule({
  declarations: [MensualitesListeComponent],
  imports: [
    SharedModule,
    RouterModule.forChild([{ path: '', component: MensualitesListeComponent }])
  ]
})
export class MensualitesModule {}
