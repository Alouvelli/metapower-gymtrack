import { Component, OnInit, AfterViewInit, ViewChild } from '@angular/core';
import { BarcodeFormat, DecodeHintType } from '@zxing/library';
import { ZXingScannerComponent } from '@zxing/ngx-scanner';
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
export class ScannerComponent implements OnInit, AfterViewInit {

  @ViewChild('zxing') zxingRef?: ZXingScannerComponent;

  formats = [BarcodeFormat.QR_CODE];

  selectedCamera?: MediaDeviceInfo;
  cameraReady = false;

  sites: Site[] = [];
  siteId: number | null = null;
  deviceKey = '';
  configMode = false;

  result?: ScanResponse;
  scanning = true;
  lastToken = '';
  lastAt = 0;

  /** deviceId de la caméra avant identifié avant le démarrage de ZXing */
  private frontCameraDeviceId: string | null = null;

  private realAudioCtx: any = null;

  constructor(private pointageSvc: PointageService, private siteSvc: SiteService) {}

  ngOnInit() {
    this.muteZxingBeep();

    this.deviceKey = localStorage.getItem(DEVICE_KEY_STORAGE) || '';
    const sid = localStorage.getItem(SITE_KEY);
    if (sid) this.siteId = Number(sid);
    this.siteSvc.liste().subscribe({
      next: s => this.sites = s,
      error: () => this.configMode = true
    });
    if (!this.siteId || !this.deviceKey) this.configMode = true;

    this.detectFrontCamera();
  }

  ngAfterViewInit() {
    this.applyZxingOptions();
  }

  /**
   * Ouvre un flux facingMode:'user' uniquement pour lire le deviceId,
   * puis le ferme immédiatement. ZXing démarrera après.
   */
  private async detectFrontCamera(): Promise<void> {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: 'user' }
      });
      this.frontCameraDeviceId =
        stream.getVideoTracks()[0]?.getSettings()?.deviceId ?? null;
      stream.getTracks().forEach(t => t.stop());
    } catch (_) {
      // HTTP sans flag ou permission refusée — on utilisera les labels
    }
    this.cameraReady = true;
  }

  /**
   * ZXing autostart avec la caméra arrière (comportement interne fixe).
   * On attend 800 ms que son autostartScanner() soit terminé, puis on switch.
   */
  onCamerasFound(cameras: MediaDeviceInfo[]): void {
    if (!cameras.length) return;

    setTimeout(() => {
      let front: MediaDeviceInfo | undefined;

      // 1. Match par deviceId récupéré avant le démarrage
      if (this.frontCameraDeviceId) {
        front = cameras.find(c => c.deviceId === this.frontCameraDeviceId);
      }
      // 2. Fallback : label Samsung "camera2 1, facing front"
      if (!front) {
        front = cameras.find(c => /front|user|selfie/i.test(c.label));
      }
      // 3. Dernier recours : dernière caméra de la liste
      if (!front && cameras.length > 1) {
        front = cameras[cameras.length - 1];
      }

      if (front && front.deviceId !== this.zxingRef?.device?.deviceId) {
        this.selectedCamera = front;
      }

      setTimeout(() => this.applyZxingOptions(), 300);
    }, 800);
  }

  private applyZxingOptions(): void {
    const s = this.zxingRef as any;
    if (!s) return;
    s.timeBetweenScansMillis = 100;
    const hints = new Map<DecodeHintType, any>([[DecodeHintType.TRY_HARDER, true]]);
    s.hints = hints;
  }

  sauverConfig() {
    if (this.siteId) localStorage.setItem(SITE_KEY, String(this.siteId));
    localStorage.setItem(DEVICE_KEY_STORAGE, this.deviceKey);
    this.configMode = false;
    setTimeout(() => this.applyZxingOptions(), 300);
  }

  onScan(qrToken: string) {
    if (!qrToken || !this.siteId || !this.scanning) return;

    const now = Date.now();
    if (qrToken === this.lastToken && now - this.lastAt < 3000) return;
    this.lastToken = qrToken;
    this.lastAt = now;

    this.scanning = false;
    this.pointageSvc.scan(qrToken, this.siteId, this.deviceKey).subscribe({
      next: r => {
        this.result = r;
        this.playFeedback(r.autorise);
        setTimeout(() => { this.result = undefined; this.scanning = true; }, 4000);
      },
      error: () => {
        this.result = { autorise: false, motifRefus: 'ERREUR_RESEAU' };
        this.playFeedback(false);
        setTimeout(() => { this.result = undefined; this.scanning = true; }, 3000);
      }
    });
  }

  motifLabel(code?: string): string {
    const labels: Record<string, string> = {
      'MEMBRE_SUSPENDU':     'Compte suspendu',
      'PAIEMENT_EN_RETARD':  'Paiement en retard',
      'TICKET_DEJA_UTILISE': 'Ticket déjà utilisé',
      'TICKET_ANNULE':       'Ticket annulé',
      'TICKET_EXPIRE':       'Ticket expiré',
      'QR_INVALIDE':         'QR code non reconnu',
      'DOUBLON_RECENT':      'Scan trop récent, patientez',
      'ERREUR_RESEAU':       'Erreur réseau'
    };
    return labels[code ?? ''] ?? code ?? 'Accès refusé';
  }

  private muteZxingBeep(): void {
    const win = window as any;
    const Original = win.AudioContext || win.webkitAudioContext;
    if (!Original) return;
    this.realAudioCtx = Original;
    const mockCtx = {
      createOscillator: () => ({
        connect: () => {}, start: () => {}, stop: () => {},
        frequency: { value: 0 }, type: 'sine', onended: null
      }),
      createGain: () => ({ connect: () => {}, gain: { value: 0 } }),
      destination: {}, currentTime: 0, state: 'running',
      suspend: () => Promise.resolve(),
      resume:  () => Promise.resolve(),
      close:   () => Promise.resolve()
    };
    win.AudioContext       = function() { return mockCtx; };
    win.webkitAudioContext = win.AudioContext;
  }

  private playFeedback(authorized: boolean): void {
    if (!this.realAudioCtx) return;
    try {
      const ctx  = new this.realAudioCtx();
      const osc  = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.connect(gain);
      gain.connect(ctx.destination);
      if (authorized) {
        osc.type = 'sine';
        gain.gain.setValueAtTime(0.45, ctx.currentTime);
        gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.9);
        osc.frequency.setValueAtTime(523, ctx.currentTime);
        osc.frequency.setValueAtTime(659, ctx.currentTime + 0.18);
        osc.frequency.setValueAtTime(784, ctx.currentTime + 0.36);
        osc.start(ctx.currentTime);
        osc.stop(ctx.currentTime + 0.9);
      } else {
        osc.type = 'sawtooth';
        gain.gain.setValueAtTime(0.5,   ctx.currentTime);
        gain.gain.setValueAtTime(0.0,   ctx.currentTime + 0.25);
        gain.gain.setValueAtTime(0.5,   ctx.currentTime + 0.35);
        gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.85);
        osc.frequency.value = 180;
        osc.start(ctx.currentTime);
        osc.stop(ctx.currentTime + 0.85);
      }
      osc.onended = () => ctx.close();
    } catch (_) {}
  }
}
