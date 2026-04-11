import { AfterViewInit, Component, OnInit, ViewChild } from '@angular/core';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatTableDataSource } from '@angular/material/table';
import { MembreService } from '../../../core/services/membre.service';
import { Membre } from '../../../core/models/models';

@Component({
  selector: 'app-membres-liste',
  templateUrl: './membres-liste.component.html',
  styleUrls: ['./membres-liste.component.scss']
})
export class MembresListeComponent implements OnInit, AfterViewInit {

  displayed = ['numeroCarte', 'nom', 'telephone', 'plan', 'statut', 'actions'];
  ds = new MatTableDataSource<Membre>([]);
  loading = false;

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  constructor(private membreSvc: MembreService) {}

  ngOnInit() {
    this.loading = true;
    this.membreSvc.liste().subscribe({
      next: data => { this.ds.data = data; this.loading = false; },
      error: () => this.loading = false
    });
  }

  ngAfterViewInit() {
    this.ds.paginator = this.paginator;
    this.ds.sort = this.sort;
  }

  filtrer(value: string) {
    this.ds.filter = value.trim().toLowerCase();
  }

  badgeClass(s?: string) {
    return s === 'ACTIF' ? 'ok' : s === 'SUSPENDU' ? 'warn' : 'expire';
  }
}
