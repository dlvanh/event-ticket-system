import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { AuthService } from '../services/auth.service';
import { Router } from '@angular/router';
import { SuccessPopupComponent } from "../success-popup/success-popup.component";

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, SuccessPopupComponent],
  templateUrl: './forgot-password.component.html',
  styleUrls: ['./forgot-password.component.css']
})
export class ForgotPasswordComponent implements OnInit {
  forgotPasswordForm!: FormGroup;
  currentStep = 1;
  message: string = '';
  errors: string[] = [];
  isLoading = false;
  registeredEmail: string = '';
  currentNewPassword = true;
  validateNewPassword = true;
  serverResponse: any = null;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) { }

  ngOnInit(): void {
    this.forgotPasswordForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      code: ['', [Validators.required]],
      newPassword: ['', [
        Validators.required,
        Validators.minLength(10),
        Validators.pattern(/^(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]).{8,}$/)
      ]],
      confirmNewPassword: ['', [Validators.required]]
    });
  }

  resetToStep1() {
    this.currentStep = 1;
    this.forgotPasswordForm.patchValue({
      code: '',
      newPassword: '',
      confirmNewPassword: ''
    });
    this.message = '';
    this.errors = [];
    this.serverResponse = null;
  }

  sendCode() {
    if (this.currentStep !== 1) return;
    const emailControl = this.forgotPasswordForm.get('email');

    if (emailControl?.valid) {
      this.isLoading = true;
      this.message = 'Đang gửi mã xác thực...';
      this.registeredEmail = emailControl.value;

      this.authService.sendCode(this.registeredEmail).subscribe({
        next: (res) => {
          this.isLoading = false;
          this.serverResponse = res;
          this.message = 'Mã xác thực đã được gửi!';
          this.currentStep = 2;
        },
        error: err => {
          this.isLoading = false;
          this.serverResponse = err;
          this.message = 'Lỗi: ' + (err?.error || 'Không xác định');
        }
      });
    } else {
      this.message = 'Email không hợp lệ!';
    }
  }

  resetPassword() {
    if (this.currentStep !== 2) return;
    const newPassword = this.forgotPasswordForm.get('newPassword')?.value;
    const confirmNewPassword = this.forgotPasswordForm.get('confirmNewPassword')?.value;
    const codeControl = this.forgotPasswordForm.get('code');

    if (codeControl?.valid) {
      if (newPassword !== confirmNewPassword) {
        this.message = 'Mật khẩu xác nhận không khớp!';
        return;
      }

      this.isLoading = true;
      this.message = 'Đang đặt lại mật khẩu...';
      this.errors = [];

      this.authService.resetPassword({
        email: this.registeredEmail,
        code: codeControl.value,
        newPassword: newPassword,
        confirmNewPassword: confirmNewPassword
      }).subscribe({
        next: () => {
          this.isLoading = false;
          this.message = 'Đặt lại mật khẩu thành công!';
          this.currentStep = 3;
        },
        error: err => {
          this.isLoading = false;
          this.serverResponse = err;
          this.message = 'Đã xảy ra lỗi.';
          if (Array.isArray(err.error)) {
            this.errors = err.error;
          } else if (typeof err.error === 'string') {
            this.errors = [err.error];
          }
        }
      });
    } else {
      this.message = 'Vui lòng nhập mã xác thực!';
    }
  }

  toggleCurrentNewPasswordVisibility() {
    this.currentNewPassword = !this.currentNewPassword;
  }

  toggleValidateNewPasswordVisibility() {
    this.validateNewPassword = !this.validateNewPassword;
  }

  onPopupClosed() {
  }

  goToHome() {
    this.router.navigate(['/']);
  }
}