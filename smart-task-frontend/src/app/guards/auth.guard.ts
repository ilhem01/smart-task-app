import { Injectable } from '@angular/core';
import { CanActivate, Router, UrlTree } from '@angular/router';

@Injectable({ providedIn: 'root' })
export class AuthGuard implements CanActivate {
  constructor(private readonly router: Router) {}

  canActivate(): boolean | UrlTree {
    const token = localStorage.getItem('token');
    const authenticated = !!token && token.trim().length > 0;

    if (authenticated) {
      return true;
    }

    return this.router.createUrlTree(['/login']);
  }
}
