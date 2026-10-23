import { Injectable } from '@angular/core';
import {
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpInterceptor,
  HttpErrorResponse
} from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Injectable()
export class AppHttpInterceptor implements HttpInterceptor {

  private readonly excludedUrls = [
    '/auth/login',
    '/auth/forgot-password',
    '/auth/reset-password',
    '/api/pointages/scan',  // utilise X-Device-Key
    '/api/pointages/device/',  // vérification de la clé tablette
    '/api/public/',            // pages publiques (ticket client, salles du scanner)
    '/files/view/',
    '/files/download/'
  ];

  constructor(private authService: AuthService, private router: Router) {}

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {

    const shouldSkipAuth = this.excludedUrls.some(url => request.url.includes(url));
    if (shouldSkipAuth) {
      return next.handle(request);
    }

    const token = localStorage.getItem('jwt-token') || this.authService.accesToken;

    if (token) {
      const cloned = request.clone({
        setHeaders: { Authorization: `Bearer ${token}` }
      });

      return next.handle(cloned).pipe(
        catchError((error: HttpErrorResponse) => {
          if (error.status === 401) {
            console.warn('[Interceptor] Token invalide ou expiré, redirection /login');
            this.authService.logout();
            this.router.navigate(['/login']);
          }
          return throwError(() => error);
        })
      );
    }

    return next.handle(request);
  }
}
