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
  // Biến hiển thị thông báo ngắn
  message: string = '';
  // Biến quản lý trạng thái chờ
  isLoading = false;
  // Biến lưu email sau bước 1
  registeredEmail: string = '';
  serverResponse: any = null;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) { }

  ngOnInit(): void {
    this.registerForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      code: ['', [Validators.required]],
      password: ['', [
        Validators.required,
        Validators.minLength(10),
        Validators.pattern(/^(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]).{8,}$/)
      ]],
      confirmPassword: ['', [Validators.required]],
      fullName: ['', [
        Validators.required,
        Validators.pattern(/^[a-zA-ZÀ-ỹ\s]+$/)]]
    });
  }

  // STEP 1: Gửi code xác thực đến email
  onSendVerification() {
    if (this.currentStep !== 1) return;
    const emailControl = this.registerForm.get('email');

    if (emailControl?.valid) {
      const emailValue = emailControl.value;

      this.isLoading = true;
      this.message = 'Đang gửi code...';

      this.authService.sendVerificationCode(emailValue).subscribe({
        next: (res: any) => {
          // Thành công => tắt loading, lưu response, hiển thị message
          this.isLoading = false;
          this.serverResponse = res;
          this.registeredEmail = emailValue; // Lưu email để dùng cho bước 2, 3
          this.message = res.message || 'Đã gửi code xác thực tới email. Vui lòng kiểm tra!';
          this.currentStep = 2; // Chuyển sang bước 2
        },
        error: (err: any) => {
          // Lỗi => tắt loading, gán response, xử lý thông báo
          this.isLoading = false;
          this.serverResponse = err;

          // Nếu server trả về mảng lỗi
          if (Array.isArray(err.error) && err.error.length > 0) {
            this.message = err.error.join('\n');
          } else {
            // Lấy err.error.message hoặc statusText
            this.message = 'Gửi code thất bại: ' + (err?.error?.message || err.statusText);
          }
        }
      });
    } else {
      // Form email chưa valid
      this.message = 'Email không hợp lệ!';
    }
  }

  // STEP 2: Kiểm tra code
  onVerifyCode() {
    if (this.currentStep !== 2) return;
    const codeControl = this.registerForm.get('code');

    if (codeControl?.valid) {
      const codeValue = codeControl.value;

      this.isLoading = true;
      this.message = 'Đang kiểm tra mã xác thực...';

      // Chỉ truyền 2 tham số: email và code
      this.authService.verifyCode(this.registeredEmail, codeValue).subscribe({
        next: (res: any) => {
          this.isLoading = false;
          this.serverResponse = res;
          if (res.error) { // Kiểm tra nếu response có chứa key "error"
            this.message = res.error;
          } else {
            this.message = res.message || 'Code xác thực đúng, mời nhập mật khẩu!';
            this.currentStep = 3;
          }
        },
        error: (err: any) => {
          // Nếu xảy ra lỗi ngoài dự kiến (ví dụ lỗi parse JSON)
          this.isLoading = false;
          this.serverResponse = err;
          this.message = 'Có lỗi xảy ra, vui lòng thử lại sau.';
        }
      });
    } else {
      // Form code chưa valid
      this.message = 'Vui lòng nhập mã code!';
    }
  }

  // STEP 3: Đăng ký tài khoản
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
      next: (res: any) => {
        this.isLoading = false;
        this.serverResponse = res;
        this.message = res || 'Đăng ký thành công!';
        this.router.navigate(['/login']);
      },
      error: (err: any) => {
        this.isLoading = false;
        this.serverResponse = err;
        let errorMsg = '';

        try {
          // Thử parse chuỗi JSON trả về từ server
          const parsedError = JSON.parse(err.error);
          if (Array.isArray(parsedError)) {
            errorMsg = parsedError.join('\n');
          } else if (parsedError.error) {
            errorMsg = parsedError.error;
          } else {
            errorMsg = err.error;
          }
        } catch (e) {
          errorMsg = err.error || err.statusText || 'Đăng ký thất bại!';
        }

        this.message = errorMsg;
      }
    });
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
