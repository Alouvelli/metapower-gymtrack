import { AfterViewInit, Component, ElementRef, OnDestroy, OnInit, ViewChild } from '@angular/core';
import {
  Chart,
  DoughnutController, BarController,
  ArcElement, BarElement,
  CategoryScale, LinearScale,
  Tooltip, Legend, Filler
} from 'chart.js';
import { DashboardService } from '../../core/services/dashboard.service';
import { SiteService } from '../../core/services/site.service';
import { AuthService } from '../../core/services/auth.service';
import { DashboardStats, GerantSiteAssignment, Site } from '../../core/models/models';

Chart.register(
  DoughnutController, BarController,
  ArcElement, BarElement,
  CategoryScale, LinearScale,
  Tooltip, Legend, Filler
);

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit, AfterViewInit, OnDestroy {

  @ViewChild('caChart')       caChartRef!:       ElementRef<HTMLCanvasElement>;
  @ViewChild('activityChart') activityChartRef!: ElementRef<HTMLCanvasElement>;

  sites:  Site[]          = [];
  siteId: number | null   = null;
  stats?: DashboardStats;
  loading = false;

  // ── Gérant-specific ─────────────────────────────────────────────────────────
  isGerant  = false;
  gerantNom = '';
  gerantSiteNom  = '';
  gerantCreneau  = '';

  private doughnutChart?: Chart;
  private barChart?:      Chart;

  constructor(
    private dash:    DashboardService,
    private siteSvc: SiteService,
    public  auth:    AuthService
  ) {}

  ngOnInit(): void {
    this.isGerant = this.auth.isGerant();

    if (!this.isGerant) {
      // Admin: load all sites + dashboard (behaviour unchanged)
      this.siteSvc.liste().subscribe(s => this.sites = s);
      this.charger();
    } else {
      // Gérant: resolve from cache or fetch
      const cached = this.auth.currentUser;
      if (cached?.siteAssignments?.length) {
        this.applyGerantProfile(cached.nom, cached.prenom, cached.siteAssignments[0]);
      } else {
        // Token exists but profile not yet cached (page refresh)
        this.auth.getCurrentUser().subscribe(user => {
          if (user.siteAssignments?.length) {
            this.applyGerantProfile(user.nom, user.prenom, user.siteAssignments[0]);
          }
        });
      }
    }
  }

  /** Populate gérant display fields and trigger the stats load. */
  private applyGerantProfile(nom?: string, prenom?: string, a?: GerantSiteAssignment): void {
    if (nom) {
      this.gerantNom = `${nom.toUpperCase()} ${prenom ?? ''}`.trim();
    }
    if (a) {
      this.siteId         = a.siteId;
      this.gerantSiteNom  = a.siteNom;
      this.gerantCreneau  = `${a.heureDebut.substring(0, 5)} – ${a.heureFin.substring(0, 5)}`;
    }
    this.charger();
  }

  ngAfterViewInit(): void {
    setTimeout(() => this.dessinerCharts(), 400);
  }

  ngOnDestroy(): void {
    this.doughnutChart?.destroy();
    this.barChart?.destroy();
  }

  changerSite(): void { this.charger(); }

  charger(): void {
    this.loading = true;
    this.dash.stats(this.siteId ?? undefined).subscribe({
      next: s => {
        this.stats = s;
        this.loading = false;
        setTimeout(() => this.dessinerCharts(), 120);
      },
      error: () => { this.loading = false; }
    });
  }

  // ─── Helpers d'affichage ───────────────────────────────────────────────────

  formatFcfa(n?: number): string {
    if (n == null) return '0 FCFA';
    return n.toLocaleString('fr-FR').replace(/,/g, ' ') + ' FCFA';
  }

  formatFcfaShort(n?: number): string {
    if (n == null) return '0';
    if (n >= 1_000_000) return (n / 1_000_000).toFixed(1).replace('.0', '') + 'M';
    if (n >= 1_000)     return (n / 1_000).toFixed(0) + 'k';
    return n.toString();
  }

  getGreeting(): string {
    const h = new Date().getHours();
    return h < 12 ? 'Bonjour' : h < 18 ? 'Bon après-midi' : 'Bonsoir';
  }

  getCurrentDate(): string {
    return new Date().toLocaleDateString('fr-FR', {
      weekday: 'long', day: 'numeric', month: 'long', year: 'numeric'
    });
  }

  getPctSuspendus(): number {
    if (!this.stats) return 0;
    const total = this.stats.membresActifs + this.stats.membresSuspendus + this.stats.membresEnRetard;
    if (!total) return 0;
    return Math.round(((this.stats.membresSuspendus + this.stats.membresEnRetard) / total) * 100);
  }

  getPctMensualites(): number {
    if (!this.stats?.caTotalMois) return 0;
    return Math.round((this.stats.caMensualitesMois / this.stats.caTotalMois) * 100);
  }

  getPctTickets(): number {
    if (!this.stats?.caTotalMois) return 0;
    return Math.round((this.stats.caTicketsMois / this.stats.caTotalMois) * 100);
  }

  // ─── Charts ───────────────────────────────────────────────────────────────

  private dessinerCharts(): void {
    this.dessinerDoughnut();
    this.dessinerBar();
  }

  private dessinerDoughnut(): void {
    if (!this.caChartRef || !this.stats) return;
    this.doughnutChart?.destroy();

    this.doughnutChart = new Chart(this.caChartRef.nativeElement, {
      type: 'doughnut',
      data: {
        labels: ['Mensualités', 'Tickets'],
        datasets: [{
          data: [this.stats.caMensualitesMois, this.stats.caTicketsMois],
          backgroundColor: ['#667eea', '#f59e0b'],
          borderWidth: 0,
          hoverOffset: 10
        }]
      },
      options: {
        responsive:          true,
        maintainAspectRatio: false,
        cutout:              '72%',
        plugins: {
          legend: { display: false },
          tooltip: {
            backgroundColor: '#2d3748',
            padding:         14,
            cornerRadius:    10,
            callbacks: {
              label: (ctx) => ' ' + (ctx.parsed as number).toLocaleString('fr-FR') + ' FCFA'
            }
          }
        },
        animation: { animateRotate: true, animateScale: true }
      }
    });
  }

  private dessinerBar(): void {
    if (!this.activityChartRef || !this.stats) return;
    this.barChart?.destroy();

    const ctx = this.activityChartRef.nativeElement.getContext('2d')!;

    const makeGrad = (r: number, g: number, b: number) => {
      const grd = ctx.createLinearGradient(0, 0, 0, 200);
      grd.addColorStop(0, `rgba(${r},${g},${b},0.88)`);
      grd.addColorStop(1, `rgba(${r},${g},${b},0.32)`);
      return grd;
    };

    this.barChart = new Chart(this.activityChartRef.nativeElement, {
      type: 'bar',
      data: {
        labels: ['Pointages', 'Tickets vendus', 'Membres actifs', 'Recouvrement %'],
        datasets: [{
          label: 'Activité',
          data: [
            this.stats.pointagesAujourdhui,
            this.stats.ticketsVendusAujourdhui,
            this.stats.membresActifs,
            this.stats.tauxRecouvrement
          ],
          backgroundColor: [
            makeGrad(59,  108, 248),
            makeGrad(245, 158, 11),
            makeGrad(16,  185, 129),
            makeGrad(118, 75,  162)
          ],
          borderWidth:    0,
          borderRadius:   8,
          borderSkipped:  false
        }]
      },
      options: {
        responsive:          true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false },
          tooltip: {
            backgroundColor: '#1e293b',
            titleColor:      'rgba(255,255,255,0.65)',
            bodyColor:       '#fff',
            padding:         12,
            cornerRadius:    9,
            displayColors:   false
          }
        },
        scales: {
          x: {
            grid:  { display: false },
            ticks: { color: '#94a3b8', font: { size: 11 } },
            border:{ display: false }
          },
          y: {
            beginAtZero: true,
            grid:  { color: 'rgba(59,108,248,0.06)', lineWidth: 1 },
            ticks: { color: '#94a3b8', font: { size: 11 } },
            border:{ display: false, dash: [4, 4] }
          }
        },
        animation: {
          duration: 700,
          easing:   'easeOutQuart',
          delay:    (ctx: any) => ctx.dataIndex * 90
        }
      }
    });
  }
}
