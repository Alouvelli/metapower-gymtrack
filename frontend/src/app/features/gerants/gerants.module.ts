import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { GerantsRoutingModule } from './gerants-routing.module';
import { GerantsListeComponent } from './liste/gerants-liste.component';
import { GerantFormComponent } from './form/gerant-form.component';

@NgModule({
  declarations: [GerantsListeComponent, GerantFormComponent],
  imports: [SharedModule, GerantsRoutingModule]
})
export class GerantsModule {}
