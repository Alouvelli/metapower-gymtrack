import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Membre } from '../models/models';

@Injectable({ providedIn: 'root' })
export class MembreService {
  private base = `${environment.apiUrl}/api/membres`;
  constructor(private http: HttpClient) {}

  liste(): Observable<Membre[]> { return this.http.get<Membre[]>(this.base); }
  get(id: number): Observable<Membre> { return this.http.get<Membre>(`${this.base}/${id}`); }
  creer(m: Membre): Observable<Membre> { return this.http.post<Membre>(this.base, m); }
  modifier(id: number, m: Membre): Observable<Membre> { return this.http.put<Membre>(`${this.base}/${id}`, m); }
  supprimer(id: number): Observable<void> { return this.http.delete<void>(`${this.base}/${id}`); }
  qrcodeUrl(id: number): string { return `${this.base}/${id}/qrcode`; }
}
