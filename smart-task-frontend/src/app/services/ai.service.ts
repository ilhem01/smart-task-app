import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AiService {
  private readonly recommendationUrl = `${environment.apiUrl}/tasks/recommendation`;

  constructor(private readonly http: HttpClient) {}

  getRecommendation(): Observable<Record<string, unknown>> {
    return this.http.get<Record<string, unknown>>(this.recommendationUrl, { headers: this.authHeaders() });
  }

  private authHeaders(): HttpHeaders {
    const token = (localStorage.getItem('token') ?? '').trim();
    return token ? new HttpHeaders({ Authorization: `Bearer ${token}` }) : new HttpHeaders();
  }
}
