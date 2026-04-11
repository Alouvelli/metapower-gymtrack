import { AfterViewInit, Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { Chart, registerables } from 'chart.js';
import { DashboardService } from '../../core/services/dashboard.service';
import { SiteService } from '../../core/services/site.service';
import { DashboardStats, Site } from '../../core/models/models';

Chart.register(...registerables);

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit, AfterViewInit {

  @ViewChild('caChart') caChartRef!: ElementRef<HTMLCanvasElement>;

  sites: Site[] = [];
  siteId: number | null = null;
  stats?: DashboardStats;
  loading = false;
  private chart?: Chart;

  constructor(private dash: DashboardService, private siteSvc: SiteService) {}

  ngOnInit() {
    this.siteSvc.liste().subscribe(s => this.sites = s);
    this.charger();
  }

  ngAfterViewInit() {
    setTimeout(() => this.dessinerChart(), 300);
  }

  changerSite() { this.charger(); }

  charger() {
    this.loading = true;
    this.dash.stats(this.siteId ?? undefined).subscribe({
      next: s => { this.stats = s; this.loading = false; this.dessinerChart(); },
      error: () => { this.loading = false; }
    });
  }

  formatFcfa(n?: number) {
    if (n == null) return '0 FCFA';
    return n.toLocaleString('fr-FR').replace(/,/g, ' ') + ' FCFA';
  }

  private dessinerChart() {
    if (!this.caChartRef || !this.stats) return;
    if (this.chart) this.chart.destroy();
    this.chart = new Chart(this.caChartRef.nativeElement, {
      type: 'doughnut',
      data: {
        labels: ['Mensualités', 'Tickets'],
        datasets: [{
          data: [this.stats.caMensualitesMois, this.stats.caTicketsMois],
          backgroundColor: ['#1F3864', '#FFC107'],
          borderWidth: 0
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { position: 'bottom' } }
      }
    });
  }
}
