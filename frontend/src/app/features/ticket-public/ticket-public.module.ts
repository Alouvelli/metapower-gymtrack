import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { TicketPublicComponent } from './ticket-public.component';

@NgModule({
  declarations: [TicketPublicComponent],
  imports: [
    CommonModule,
    RouterModule.forChild([{ path: '', component: TicketPublicComponent }])
  ]
})
export class TicketPublicModule {}
