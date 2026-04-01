import { Component } from '@angular/core';

import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-register',
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss']
})
export class RegisterComponent {
  username = '';
  password = '';
  successMessage = '';
  errorMessage = '';
  isLoading = false;

  constructor(private readonly authService: AuthService) {}

  onRegister(): void {
    this.successMessage = '';
    this.errorMessage = '';

    if (!this.username.trim() || !this.password.trim()) {
      this.errorMessage = 'Username and password are required.';
      return;
    }

    this.isLoading = true;
    this.authService.register(this.username.trim(), this.password).subscribe({
      next: (message: string) => {
        this.isLoading = false;
        this.successMessage = message || 'Registration successful.';
        this.password = '';
      },
      error: (error: Error) => {
        this.isLoading = false;
        this.errorMessage = error.message || 'Registration failed.';
      }
    });
  }
}
