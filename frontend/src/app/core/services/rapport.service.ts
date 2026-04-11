import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class RapportService {
  private base = `${environment.apiUrl}/api/rapports`;
  constructor(private http: HttpClient) {}

  mensualitesExcel(mois: string): Observable<Blob> {
    return this.http.get(`${this.base}/mensualites/excel`, {
      params: new HttpParams().set('mois', mois), responseType: 'blob'
    });
  }

  mensualitesPdf(mois: string): Observable<Blob> {
    return this.http.get(`${this.base}/mensualites/pdf`, {
      params: new HttpParams().set('mois', mois), responseType: 'blob'
    });
  }

  ticketsExcel(mois: string, siteId?: number): Observable<Blob> {
    let p = new HttpParams().set('mois', mois);
    if (siteId != null) p = p.set('siteId', siteId);
    return this.http.get(`${this.base}/tickets/excel`, { params: p, responseType: 'blob' });
  }

  download(blob: Blob, filename: string) {
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url; a.download = filename; a.click();
    window.URL.revokeObjectURL(url);
  }
}
