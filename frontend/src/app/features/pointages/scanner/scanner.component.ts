import { Component, OnInit } from '@angular/core';
import { BarcodeFormat } from '@zxing/library';
import { PointageService } from '../../../core/services/pointage.service';
import { SiteService } from '../../../core/services/site.service';
import { ScanResponse, Site } from '../../../core/models/models';

const SITE_KEY = 'scanner-site-id';
const DEVICE_KEY_STORAGE = 'scanner-device-key';

@Component({
  selector: 'app-scanner',
  templateUrl: './scanner.component.html',
  styleUrls: ['./scanner.component.scss']
})
export class ScannerComponent implements OnInit {

  formats = [BarcodeFormat.QR_CODE];
  sites: Site[] = [];
  siteId: number | null = null;
  deviceKey = '';
  configMode = false;

  result?: ScanResponse;
  scanning = true;
  lastToken = '';
  lastAt = 0;

  constructor(private pointageSvc: PointageService, private siteSvc: SiteService) {}

  ngOnInit() {
    this.deviceKey = localStorage.getItem(DEVICE_KEY_STORAGE) || '';
    const sid = localStorage.getItem(SITE_KEY);
    if (sid) this.siteId = Number(sid);
    this.siteSvc.liste().subscribe({
      next: s => this.sites = s,
      error: () => this.configMode = true
    });
    if (!this.siteId || !this.deviceKey) this.configMode = true;
  }

  sauverConfig() {
    if (this.siteId) localStorage.setItem(SITE_KEY, String(this.siteId));
    localStorage.setItem(DEVICE_KEY_STORAGE, this.deviceKey);
    this.configMode = false;
  }

  onScan(qrToken: string) {
    if (!qrToken || !this.siteId || !this.scanning) return;

    // Anti-doublon local 3s
    const now = Date.now();
    if (qrToken === this.lastToken && now - this.lastAt < 3000) return;
    this.lastToken = qrToken;
    this.lastAt = now;

    this.scanning = false;
    this.pointageSvc.scan(qrToken, this.siteId, this.deviceKey).subscribe({
      next: r => {
        this.result = r;
        setTimeout(() => { this.result = undefined; this.scanning = true; }, 4000);
      },
      error: () => {
        this.result = { autorise: false, motifRefus: 'ERREUR_RESEAU' };
        setTimeout(() => { this.result = undefined; this.scanning = true; }, 3000);
      }
    });
  }
}
