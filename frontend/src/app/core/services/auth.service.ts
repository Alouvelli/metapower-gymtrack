import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CurrentUser } from '../models/models';

const TOKEN_KEY = 'jwt-token';

@Injectable({ providedIn: 'root' })
export class AuthService {

  accesToken: string | null = null;

  private currentUserSubject = new BehaviorSubject<CurrentUser | null>(null);
  /** Observable du profil utilisateur courant (null avant login ou après logout). */
  readonly currentUser$ = this.currentUserSubject.asObservable();

  constructor(private http: HttpClient) {
    this.accesToken = localStorage.getItem(TOKEN_KEY);
  }

  login(username: string, password: string): Observable<{ 'access-token': string }> {
    const body = new HttpParams()
      .set('username', username)
      .set('password', password);

    return this.http.post<{ 'access-token': string }>(
      `${environment.apiUrl}/auth/login`,
      body.toString(),
      { headers: { 'Content-Type': 'application/x-www-form-urlencoded' } }
    ).pipe(
      tap(res => {
        this.accesToken = res['access-token'];
        localStorage.setItem(TOKEN_KEY, this.accesToken);
        // Pré-charge le profil dans le cache après login
        this.getCurrentUser().subscribe();
      })
    );
  }

  logout(): void {
    this.accesToken = null;
    localStorage.removeItem(TOKEN_KEY);
    this.currentUserSubject.next(null);
  }

  isAuthenticated(): boolean {
    return !!this.accesToken;
  }

  // ── Décodage JWT ────────────────────────────────────────────────────────────

  /** Décode le payload base64url du JWT sans vérification de signature. */
  private decodeToken(): any {
    if (!this.accesToken) return null;
    try {
      const payload = this.accesToken.split('.')[1];
      // base64url → base64 standard
      const b64 = payload.replace(/-/g, '+').replace(/_/g, '/');
      const padded = b64.padEnd(b64.length + (4 - b64.length % 4) % 4, '=');
      return JSON.parse(atob(padded));
    } catch {
      return null;
    }
  }

  /** Retourne le premier rôle du token JWT (ex. 'ROLE_ADMIN', 'ROLE_GERANT'). */
  getUserRole(): string | null {
    const payload = this.decodeToken();
    if (!payload?.scope) return null;
    return payload.scope.split(' ')[0] ?? null;
  }

  isAdmin(): boolean {
    return this.getUserRole() === 'ROLE_ADMIN';
  }

  isGerant(): boolean {
    return this.getUserRole() === 'ROLE_GERANT';
  }

  // ── Profil utilisateur courant ──────────────────────────────────────────────

  /**
   * Récupère le profil complet depuis /api/users/me et le met en cache.
   * Appelé automatiquement après le login ; peut être rappelé si besoin.
   */
  getCurrentUser(): Observable<CurrentUser> {
    return this.http.get<CurrentUser>(`${environment.apiUrl}/api/users/me`).pipe(
      tap(user => this.currentUserSubject.next(user))
    );
  }

  /** Valeur synchrone du cache (null si pas encore chargé). */
  get currentUser(): CurrentUser | null {
    return this.currentUserSubject.value;
  }
}
