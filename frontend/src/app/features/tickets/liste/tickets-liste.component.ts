import { Component, OnInit, ViewChild, AfterViewInit } from '@angular/core';
import { MatPaginator } from '@angular/material/paginator';
import { MatTableDataSource } from '@angular/material/table';
import { TicketService } from '../../../core/services/ticket.service';
import { TicketEntree } from '../../../core/models/models';

@Component({
  selector: 'app-tickets-liste',
  templateUrl: './tickets-liste.component.html',
  styleUrls: ['./tickets-liste.component.scss']
})
export class TicketsListeComponent implements OnInit, AfterViewInit {
  displayed = ['numero', 'site', 'date', 'tarif', 'prix', 'mode', 'client', 'statut'];
  ds = new MatTableDataSource<TicketEntree>([]);
  @ViewChild(MatPaginator) paginator!: MatPaginator;

  constructor(private ticketSvc: TicketService) {}

  ngOnInit() {
    this.ticketSvc.liste().subscribe(t => this.ds.data = t);
  }
  ngAfterViewInit() { this.ds.paginator = this.paginator; }

  badge(s?: string) {
    return s === 'VALIDE' ? 'ok' : s === 'UTILISE' ? 'used' : 'warn';
  }
}
