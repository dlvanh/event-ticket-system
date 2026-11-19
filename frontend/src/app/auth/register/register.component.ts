import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { AuthService } from '../services/auth.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.css']
})
export class RegisterComponent implements OnInit {
  registerForm!: FormGroup;
  currentPassword = true;
  validatePassword = true;

  currentStep = 1;
  message = '';
  isLoading = false;
  registeredEmail = '';
  serverResponse: any = null;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.registerForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      code: ['', [Validators.required]],
      password: ['', [
        Validators.required,
        Validators.minLength(10),
        Validators.pattern(/^(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]).{10,}$/)
      ]],
      confirmPassword: ['', [Validators.required]],
      fullName: ['', [
        Validators.required,
        Validators.pattern(/^[a-zA-ZÀ-ỹ\s]+$/)
      ]]
    });
  }

  onSendVerification() {
    if (this.currentStep !== 1) return;
    const emailControl = this.registerForm.get('email');

    if (emailControl?.valid) {
      const emailValue = emailControl.value;
      this.isLoading = true;
      this.message = 'Đang gửi code...';

      this.authService.sendVerificationCode(emailValue).subscribe({
        next: (res) => {
          this.isLoading = false;
          this.serverResponse = res;
          this.registeredEmail = emailValue;
          this.message = res?.message || 'Đã gửi code xác thực tới email. Vui lòng kiểm tra!';
          this.currentStep = 2;
        },
        error: (err) => {
          this.isLoading = false;
          this.serverResponse = err;
          this.message = 'Gửi code thất bại: ' + this.extractErrorMessage(err);
        }
      });
    } else {
      this.message = 'Email không hợp lệ!';
    }
  }

  onVerifyCode() {
    if (this.currentStep !== 2) return;
    const codeControl = this.registerForm.get('code');

    if (codeControl?.valid) {
      const codeValue = codeControl.value;
      this.isLoading = true;
      this.message = 'Đang kiểm tra mã xác thực...';

      this.authService.verifyCode(this.registeredEmail, codeValue).subscribe({
        next: (res) => {
          this.isLoading = false;
          this.serverResponse = res;
          this.message = res?.message || 'Code xác thực đúng, mời nhập mật khẩu!';
          this.currentStep = 3;
        },
        error: (err) => {
          this.isLoading = false;
          this.serverResponse = err;
          this.message = this.extractErrorMessage(err) || 'Mã xác thực không hợp lệ!';
        }
      });
    } else {
      this.message = 'Vui lòng nhập mã code!';
    }
  }

  onRegister() {
    if (this.currentStep !== 3) return;
    const password = this.registerForm.get('password')?.value;
    const confirmPassword = this.registerForm.get('confirmPassword')?.value;
    const fullName = this.registerForm.get('fullName')?.value;

    if (password !== confirmPassword) {
      this.message = 'Mật khẩu không trùng khớp!';
      return;
    }

    this.isLoading = true;
    this.message = 'Đang đăng ký...';

    this.authService.register(this.registeredEmail, password, confirmPassword, fullName).subscribe({
      next: (res) => {
        this.isLoading = false;
        this.serverResponse = res;
        this.message = res?.message || 'Đăng ký thành công!';
        this.router.navigate(['/login']);
      },
      error: (err) => {
        this.isLoading = false;
        this.serverResponse = err;
        this.message = this.extractErrorMessage(err) || 'Đăng ký thất bại!';
      }
    });
  }

  private extractErrorMessage(err: any): string {
    const body = err?.error;
    if (!body) return err?.statusText || 'Có lỗi xảy ra';
    if (typeof body === 'string') return body;
    if (Array.isArray(body)) return body.join('\n');
    if (body.errors && typeof body.errors === 'object') {
      return Object.values(body.errors).join('\n');
    }
    return body.message || body.error || err?.statusText || 'Có lỗi xảy ra';
  }

  toggleCurrentPasswordVisibility() {
    this.currentPassword = !this.currentPassword;
  }

  toggleValidatePasswordVisibility() {
    this.validatePassword = !this.validatePassword;
  }

  goToHome() {
    this.router.navigate(['/']);
  }
}
