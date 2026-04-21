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
import { DashboardStats, Site } from '../../core/models/models';

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

  @ViewChild('caChart') caChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('activityChart') activityChartRef!: ElementRef<HTMLCanvasElement>;

  sites: Site[] = [];
  siteId: number | null = null;
  stats?: DashboardStats;
  loading = false;
  private doughnutChart?: Chart;
  private barChart?: Chart;

  constructor(private dash: DashboardService, private siteSvc: SiteService) {}

  ngOnInit() {
    this.siteSvc.liste().subscribe(s => this.sites = s);
    this.charger();
  }

  ngAfterViewInit() {
    setTimeout(() => this.dessinerCharts(), 400);
  }

  ngOnDestroy() {
    this.doughnutChart?.destroy();
    this.barChart?.destroy();
  }

  changerSite() { this.charger(); }

  charger() {
    this.loading = true;
    this.dash.stats(this.siteId ?? undefined).subscribe({
      next: s => {
        this.stats = s;
        this.loading = false;
        setTimeout(() => this.dessinerCharts(), 100);
      },
      error: () => { this.loading = false; }
    });
  }

  // ─── Helpers d'affichage ───
  formatFcfa(n?: number): string {
    if (n == null) return '0 FCFA';
    return n.toLocaleString('fr-FR').replace(/,/g, ' ') + ' FCFA';
  }

  formatFcfaShort(n?: number): string {
    if (n == null) return '0';
    if (n >= 1_000_000) return (n / 1_000_000).toFixed(1).replace('.0', '') + 'M';
    if (n >= 1_000) return (n / 1_000).toFixed(0) + 'k';
    return n.toString();
  }

  getGreeting(): string {
    const h = new Date().getHours();
    return h < 12 ? 'Bonjour' : h < 18 ? 'Bon après-midi' : 'Bonsoir';
  }

  getCurrentDate(): string {
    return new Date().toLocaleDateString('fr-FR', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
  }

  getPctSuspendus(): number {
    if (!this.stats) return 0;
    const total = this.stats.membresActifs + this.stats.membresSuspendus + this.stats.membresEnRetard;
    if (!total) return 0;
    return Math.round(((this.stats.membresSuspendus + this.stats.membresEnRetard) / total) * 100);
  }

  getPctMensualites(): number {
    if (!this.stats || !this.stats.caTotalMois) return 0;
    return Math.round((this.stats.caMensualitesMois / this.stats.caTotalMois) * 100);
  }

  getPctTickets(): number {
    if (!this.stats || !this.stats.caTotalMois) return 0;
    return Math.round((this.stats.caTicketsMois / this.stats.caTotalMois) * 100);
  }

  // ─── CHARTS ───
  private dessinerCharts() {
    this.dessinerDoughnut();
    this.dessinerBar();
  }

  private dessinerDoughnut() {
    if (!this.caChartRef || !this.stats) return;
    if (this.doughnutChart) this.doughnutChart.destroy();

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
        responsive: true,
        maintainAspectRatio: false,
        cutout: '72%',
        plugins: {
          legend: { display: false },
          tooltip: {
            backgroundColor: '#2d3748',
            padding: 14,
            cornerRadius: 10,
            callbacks: {
              label: (ctx) => ' ' + (ctx.parsed as number).toLocaleString('fr-FR') + ' FCFA'
            }
          }
        },
        animation: { animateRotate: true, animateScale: true }
      }
    });
  }

  private dessinerBar() {
    if (!this.activityChartRef || !this.stats) return;
    if (this.barChart) this.barChart.destroy();

    const ctx = this.activityChartRef.nativeElement.getContext('2d')!;
    const gradient = ctx.createLinearGradient(0, 0, 0, 250);
    gradient.addColorStop(0, 'rgba(102, 126, 234, 0.85)');
    gradient.addColorStop(1, 'rgba(118, 75, 162, 0.4)');

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
          backgroundColor: gradient,
          borderColor: '#667eea',
          borderWidth: 2,
          borderRadius: 8,
          borderSkipped: false
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false },
          tooltip: {
            backgroundColor: '#2d3748',
            padding: 12,
            cornerRadius: 8
          }
        },
        scales: {
          x: { grid: { display: false }, ticks: { color: '#718096', font: { size: 11 } } },
          y: { beginAtZero: true, grid: { color: 'rgba(0,0,0,0.05)' }, ticks: { color: '#718096', font: { size: 11 } } }
        }
      }
    });
  }
}
