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
  creer(s: Site): Observable<Site> { return this.http.post<Site>(this.base, s); }
  modifier(id: number, s: Site): Observable<Site> { return this.http.put<Site>(`${this.base}/${id}`, s); }
  supprimer(id: number): Observable<void> { return this.http.delete<void>(`${this.base}/${id}`); }
}
