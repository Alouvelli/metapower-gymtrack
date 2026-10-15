import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { GerantsListeComponent } from './liste/gerants-liste.component';

const routes: Routes = [
  { path: '', component: GerantsListeComponent }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class GerantsRoutingModule {}
