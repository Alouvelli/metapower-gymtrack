import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { ZXingScannerModule } from '@zxing/ngx-scanner';
import { MaterialModule } from '../../../shared/material.module';
import { ScannerComponent } from './scanner.component';

@NgModule({
  declarations: [ScannerComponent],
  imports: [
    CommonModule, FormsModule, MaterialModule, ZXingScannerModule,
    RouterModule.forChild([{ path: '', component: ScannerComponent }])
  ]
})
export class ScannerModule {}
