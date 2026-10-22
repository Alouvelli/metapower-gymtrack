import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Gerant, GerantStats } from '../models/models';

@Injectable({ providedIn: 'root' })
export class GerantService {

  private base = `${environment.apiUrl}/api/gerants`;

  constructor(private http: HttpClient) {}

  liste(): Observable<Gerant[]> {
    return this.http.get<Gerant[]>(this.base);
  }

  listeBySite(siteId: number): Observable<Gerant[]> {
    return this.http.get<Gerant[]>(`${this.base}/site/${siteId}`);
  }

  detail(id: number): Observable<Gerant> {
    return this.http.get<Gerant>(`${this.base}/${id}`);
  }

  creer(dto: Partial<Gerant> & { password: string }): Observable<Gerant> {
    // Normalise les créneaux HH:mm → HH:mm:ss attendu par LocalTime Java
    const payload = {
      ...dto,
      heureDebut: this.toHHmmss(dto.heureDebut!),
      heureFin:   this.toHHmmss(dto.heureFin!),
    };
    return this.http.post<Gerant>(this.base, payload);
  }

  updateHoraire(id: number, heureDebut: string, heureFin: string): Observable<Gerant> {
    return this.http.put<Gerant>(`${this.base}/${id}/horaire`, {
      heureDebut: this.toHHmmss(heureDebut),
      heureFin:   this.toHHmmss(heureFin),
    });
  }

  desactiver(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }

  /** Stats par gérant pour un mois donné (format YYYY-MM, ex. "2026-05"). */
  statsParGerant(mois: string): Observable<GerantStats[]> {
    return this.http.get<GerantStats[]>(
      `${environment.apiUrl}/api/dashboard/gerants/stats`,
      { params: { mois } }
    );
  }

  /** Convertit "HH:mm" (input[type=time]) en "HH:mm:00" pour LocalTime Java. */
  toHHmmss(t: string): string {
    return t?.length === 5 ? `${t}:00` : t;
  }

  /** Convertit "HH:mm:ss" → "HH:mm" pour l'affichage dans les inputs. */
  toHHmm(t: string): string {
    return t?.substring(0, 5) ?? '';
  }
}
