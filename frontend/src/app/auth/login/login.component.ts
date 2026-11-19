import { Component, OnInit,ViewChild  } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { AuthService } from '../services/auth.service';
import { Router } from '@angular/router';
import { ToastNotificationComponent } from '../../user/pop-up/toast-notification/toast-notification.component';
@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, ToastNotificationComponent],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit {
  loginForm!: FormGroup;
  hidePassword = true;
  loginErrorMessage: string | null = null;
  errorMessage: string = '';
  @ViewChild('notification') notification!: ToastNotificationComponent;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required,]],
      rememberMe: [false]
    });
  }

  onSubmit() {
    if (this.loginForm.valid) {
      const { email, password } = this.loginForm.value;

      this.authService.login(email, password).subscribe({
        next: (res: any) => {
          const token = res?.data?.token ?? res?.token;
          if (token) {
            this.authService.setToken(token);

            const redirectPath = sessionStorage.getItem('redirectAfterLogin');

            if (redirectPath && this.authService.isCustomer()) {
              sessionStorage.removeItem('redirectAfterLogin');
              this.router.navigateByUrl(redirectPath);
              return;
            }

            if (this.authService.isAdmin()) {
              this.router.navigate(['/admin']);
            } else if (this.authService.isSeller()) {
              this.router.navigate(['/organizer']);
            } else {
              this.router.navigate(['/']);
            }
          }
        },
        error: () => {
          this.notification.showNotification('Sai tài khoản hoặc mật khẩu!', 5000, 'error');
        }
      });
    }
  }

  togglePasswordVisibility() {
    this.hidePassword = !this.hidePassword;
  }

  loginWithGoogle() {
    window.location.href = '/oauth2/authorization/google';
  }

  forgotPassword() {
    this.router.navigate(['/forgot-password']);
  }

  createAccount() {
    this.router.navigate(['/register']);
  }

  goToHome() {
    this.router.navigate(['/']);
  }

  onNotificationClose() {
  }}
