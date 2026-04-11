import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { MembresListeComponent } from './liste/membres-liste.component';
import { MembreDetailComponent } from './detail/membre-detail.component';
import { MembreFormComponent } from './nouveau/membre-form.component';

@NgModule({
  declarations: [MembresListeComponent, MembreDetailComponent, MembreFormComponent],
  imports: [
    SharedModule,
    RouterModule.forChild([
      { path: '', component: MembresListeComponent },
      { path: 'nouveau', component: MembreFormComponent },
      { path: ':id', component: MembreDetailComponent },
      { path: ':id/modifier', component: MembreFormComponent }
    ])
  ]
})
export class MembresModule {}
