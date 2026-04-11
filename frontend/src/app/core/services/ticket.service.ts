import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { TarifTicket, TicketEntree, TicketVenteResponse } from '../models/models';

@Injectable({ providedIn: 'root' })
export class TicketService {
  private base = `${environment.apiUrl}/api/tickets`;
  private tarifBase = `${environment.apiUrl}/api/tarifs-tickets`;
  constructor(private http: HttpClient) {}

  // Tarifs
  listeTarifs(): Observable<TarifTicket[]> { return this.http.get<TarifTicket[]>(this.tarifBase); }
  creerTarif(t: TarifTicket): Observable<TarifTicket> { return this.http.post<TarifTicket>(this.tarifBase, t); }
  modifierTarif(id: number, t: TarifTicket): Observable<TarifTicket> { return this.http.put<TarifTicket>(`${this.tarifBase}/${id}`, t); }
  supprimerTarif(id: number): Observable<void> { return this.http.delete<void>(`${this.tarifBase}/${id}`); }

  // Tickets
  liste(): Observable<TicketEntree[]> { return this.http.get<TicketEntree[]>(this.base); }
  vendre(payload: any): Observable<TicketVenteResponse> {
    return this.http.post<TicketVenteResponse>(this.base, payload);
  }
  recuUrl(id: number): string { return `${this.base}/${id}/recu`; }
}
