import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { RapportsComponent } from './rapports.component';

@NgModule({
  declarations: [RapportsComponent],
  imports: [
    SharedModule,
    RouterModule.forChild([{ path: '', component: RapportsComponent }])
  ]
})
export class RapportsModule {}
