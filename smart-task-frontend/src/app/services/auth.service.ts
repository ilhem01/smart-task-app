import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, catchError, tap, throwError } from 'rxjs';

import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly authUrl = `${environment.apiUrl}/auth`;

  constructor(
    private readonly http: HttpClient,
    private readonly router: Router
  ) {}

  register(username: string, password: string): Observable<string> {
    return this.http
      .post(this.authUrl + '/register', { username, password }, { responseType: 'text' })
      .pipe(catchError((error) => this.handleError(error)));
  }

  login(username: string, password: string): Observable<{ token: string }> {
    return this.http.post<{ token: string }>(this.authUrl + '/login', { username, password }).pipe(
      tap((response) => {
        localStorage.setItem('token', response.token);
      }),
      catchError((error) => this.handleError(error))
    );
  }

  logout(): void {
    localStorage.removeItem('token');
    this.router.navigate(['/login']);
  }

  private handleError(error: HttpErrorResponse) {
    const message =
      typeof error.error === 'string'
        ? error.error
        : error.error?.message || `Request failed (${error.status})`;
    return throwError(() => new Error(message));
  }
}
