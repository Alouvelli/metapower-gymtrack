import { Component, OnInit, ViewChild, AfterViewInit } from '@angular/core';
import { MatPaginator } from '@angular/material/paginator';
import { MatTableDataSource } from '@angular/material/table';
import { Router } from '@angular/router';
import { MensualiteService } from '../../core/services/mensualite.service';
import { TicketService } from '../../core/services/ticket.service';
import { AuthService } from '../../core/services/auth.service';
import { Mensualite, TicketEntree } from '../../core/models/models';

@Component({
  selector: 'app-mes-transactions',
  templateUrl: './mes-transactions.component.html',
  styleUrls: ['./mes-transactions.component.scss']
})
export class MesTransactionsComponent implements OnInit, AfterViewInit {

  activeTab: 'mensualites' | 'tickets' = 'mensualites';

  // ── Mensualités ──────────────────────────────────────────────────────────────
  mensCols = ['membre', 'mois', 'montant', 'date', 'mode', 'statut'];
  mensDs   = new MatTableDataSource<Mensualite>([]);
  @ViewChild('mensPaginator') mensPaginator!: MatPaginator;
  loadingMens = false;

  // ── Tickets ──────────────────────────────────────────────────────────────────
  tickCols = ['numero', 'date', 'prix', 'mode', 'client', 'statut'];
  tickDs   = new MatTableDataSource<TicketEntree>([]);
  @ViewChild('tickPaginator') tickPaginator!: MatPaginator;
  loadingTick = false;

  constructor(
    private mensSvc: MensualiteService,
    private tickSvc: TicketService,
    public  auth:    AuthService,
    private router:  Router
  ) {}

  ngOnInit(): void {
    // Admins have no business here — redirect to full reports
    if (this.auth.isAdmin()) {
      this.router.navigate(['/rapports']);
      return;
    }
    this.chargerMensualites();
    this.chargerTickets();
  }

  ngAfterViewInit(): void {
    this.mensDs.paginator = this.mensPaginator;
    this.tickDs.paginator = this.tickPaginator;
  }

  // ── Data loading ─────────────────────────────────────────────────────────────

  chargerMensualites(): void {
    this.loadingMens = true;
    this.mensSvc.liste().subscribe({
      next:  d => { this.mensDs.data = d; this.loadingMens = false; },
      error: () => { this.loadingMens = false; }
    });
  }

  chargerTickets(): void {
    this.loadingTick = true;
    this.tickSvc.liste().subscribe({
      next:  t => { this.tickDs.data = t; this.loadingTick = false; },
      error: () => { this.loadingTick = false; }
    });
  }

  // ── Quick stats ───────────────────────────────────────────────────────────────

  get totalMens(): number { return this.mensDs.data.length; }
  get caMens():    number { return this.mensDs.data.reduce((s, m) => s + (m.montant ?? 0), 0); }
  get totalTick(): number { return this.tickDs.data.length; }
  get caTick():    number { return this.tickDs.data.reduce((s, t) => s + (t.prix   ?? 0), 0); }

  // ── User info getters ─────────────────────────────────────────────────────────

  get displayName(): string {
    const u = this.auth.currentUser;
    if (!u) return '';
    return u.nom ? `${u.nom.toUpperCase()} ${u.prenom ?? ''}`.trim() : u.username;
  }

  get siteNom(): string {
    return this.auth.currentUser?.siteAssignments?.[0]?.siteNom ?? '';
  }

  get creneau(): string {
    const a = this.auth.currentUser?.siteAssignments?.[0];
    if (!a) return '';
    return `${a.heureDebut.substring(0, 5)} – ${a.heureFin.substring(0, 5)}`;
  }

  // ── Formatting ────────────────────────────────────────────────────────────────

  formatFcfa(n: number): string {
    if (n >= 1_000_000) return (n / 1_000_000).toFixed(1).replace('.0', '') + ' M';
    if (n >= 1_000)     return Math.round(n / 1_000) + ' k';
    return n.toLocaleString('fr-FR');
  }
}
