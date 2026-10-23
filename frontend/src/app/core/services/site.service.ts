import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Site } from '../models/models';

@Injectable({ providedIn: 'root' })
export class SiteService {
  private base = `${environment.apiUrl}/api/sites`;
  constructor(private http: HttpClient) {}

  liste(): Observable<Site[]> { return this.http.get<Site[]>(this.base); }
  /** Salles actives (id + nom) sans connexion — écran de configuration du scanner tablette. */
  listePublique(): Observable<Site[]> { return this.http.get<Site[]>(`${environment.apiUrl}/api/public/sites`); }
  creer(s: Site): Observable<Site> { return this.http.post<Site>(this.base, s); }
  modifier(id: number, s: Site): Observable<Site> { return this.http.put<Site>(`${this.base}/${id}`, s); }
  supprimer(id: number): Observable<void> { return this.http.delete<void>(`${this.base}/${id}`); }
}
