import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';

import { API_URL } from '../api-url';
import { Task } from '../models/task.model';

@Injectable({ providedIn: 'root' })
export class TaskService {
  private readonly taskUrl = `${API_URL}/tasks`;
  recommendation: Record<string, unknown> | null = null;

  constructor(private readonly http: HttpClient) {}

  getTasks(): Observable<Task[]> {
    return this.http.get<Task[]>(this.taskUrl, { headers: this.authHeaders() });
  }

  getRecommendation(): Observable<Record<string, unknown>> {
    return this.http
      .get<Record<string, unknown>>(`${this.taskUrl}/recommendation`, { headers: this.authHeaders() })
      .pipe(tap((result) => (this.recommendation = result)));
  }

  /** POST {API_URL}/tasks — creates a task; list should be reloaded by the caller after success. */
  createTask(task: Task): Observable<Task> {
    return this.http.post<Task>(this.taskUrl, task, {
      headers: this.authHeaders().set('Content-Type', 'application/json')
    });
  }

  /** PUT {API_URL}/tasks/{id} — full task update (including completed). */
  updateTask(id: number, task: Task): Observable<Task> {
    return this.http.put<Task>(`${this.taskUrl}/${id}`, task, {
      headers: this.authHeaders().set('Content-Type', 'application/json')
    });
  }

  deleteTask(id: number): Observable<void> {
    return this.http.delete<void>(`${this.taskUrl}/${id}`, { headers: this.authHeaders() });
  }

  private authHeaders(): HttpHeaders {
    const token = (localStorage.getItem('token') ?? '').trim();
    return token ? new HttpHeaders({ Authorization: `Bearer ${token}` }) : new HttpHeaders();
  }
}
