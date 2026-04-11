import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { DashboardStats } from '../models/models';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private base = `${environment.apiUrl}/api/dashboard`;
  constructor(private http: HttpClient) {}

  stats(siteId?: number): Observable<DashboardStats> {
    let params = new HttpParams();
    if (siteId != null) params = params.set('siteId', siteId);
    return this.http.get<DashboardStats>(`${this.base}/stats`, { params });
  }
}
