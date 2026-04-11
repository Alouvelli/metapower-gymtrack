import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { ParametresComponent } from './parametres.component';

@NgModule({
  declarations: [ParametresComponent],
  imports: [
    SharedModule,
    RouterModule.forChild([{ path: '', component: ParametresComponent }])
  ]
})
export class ParametresModule {}
