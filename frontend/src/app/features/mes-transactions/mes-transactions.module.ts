import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { MesTransactionsComponent } from './mes-transactions.component';

@NgModule({
  declarations: [MesTransactionsComponent],
  imports: [
    SharedModule,
    RouterModule.forChild([{ path: '', component: MesTransactionsComponent }])
  ]
})
export class MesTransactionsModule {}
