import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { SitesComponent } from './sites.component';

@NgModule({
  declarations: [SitesComponent],
  imports: [
    SharedModule,
    RouterModule.forChild([{ path: '', component: SitesComponent }])
  ]
})
export class SitesModule {}
