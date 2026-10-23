import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PointageMembre, ScanResponse } from '../models/models';

@Injectable({ providedIn: 'root' })
export class PointageService {
  private base = `${environment.apiUrl}/api/pointages`;
  constructor(private http: HttpClient) {}

  scan(qrToken: string, siteId: number, deviceKey: string): Observable<ScanResponse> {
    return this.http.post<ScanResponse>(
      `${this.base}/scan`,
      { qrToken, siteId, scanSource: 'TABLETTE' },
      { headers: { 'X-Device-Key': deviceKey } }
    );
  }

  /** Vérifie la clé de la tablette (erreur 401 si elle est fausse). */
  verifierCleDevice(deviceKey: string): Observable<{ valide: boolean }> {
    return this.http.get<{ valide: boolean }>(`${this.base}/device/verifier`, { headers: { 'X-Device-Key': deviceKey } });
  }

  jour(siteId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.base}/site/${siteId}/jour`);
  }

  parMembre(membreId: number): Observable<PointageMembre[]> {
    return this.http.get<PointageMembre[]>(`${this.base}/membre/${membreId}`);
  }
}
