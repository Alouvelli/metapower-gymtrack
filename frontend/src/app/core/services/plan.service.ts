import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PlanAbonnement } from '../models/models';

@Injectable({ providedIn: 'root' })
export class PlanService {
  private base = `${environment.apiUrl}/api/plans`;
  constructor(private http: HttpClient) {}

  liste(): Observable<PlanAbonnement[]> { return this.http.get<PlanAbonnement[]>(this.base); }
  creer(p: PlanAbonnement): Observable<PlanAbonnement> { return this.http.post<PlanAbonnement>(this.base, p); }
  modifier(id: number, p: PlanAbonnement): Observable<PlanAbonnement> { return this.http.put<PlanAbonnement>(`${this.base}/${id}`, p); }
  supprimer(id: number): Observable<void> { return this.http.delete<void>(`${this.base}/${id}`); }
}
