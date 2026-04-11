import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Mensualite } from '../models/models';

@Injectable({ providedIn: 'root' })
export class MensualiteService {
  private base = `${environment.apiUrl}/api/mensualites`;
  constructor(private http: HttpClient) {}

  liste(): Observable<Mensualite[]> { return this.http.get<Mensualite[]>(this.base); }
  parMembre(membreId: number): Observable<Mensualite[]> {
    return this.http.get<Mensualite[]>(`${this.base}/membre/${membreId}`);
  }
  enregistrerPaiement(payload: any): Observable<Mensualite> {
    return this.http.post<Mensualite>(this.base, payload);
  }
}
