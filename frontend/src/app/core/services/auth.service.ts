import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';

const TOKEN_KEY = 'jwt-token';

@Injectable({ providedIn: 'root' })
export class AuthService {

  accesToken: string | null = null;

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
      })
    );
  }

  logout(): void {
    this.accesToken = null;
    localStorage.removeItem(TOKEN_KEY);
  }

  isAuthenticated(): boolean {
    return !!this.accesToken;
  }
}
