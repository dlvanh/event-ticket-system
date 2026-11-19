import { Component, OnInit, ViewChild, ElementRef } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-register-organizer',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './register-organizer.component.html',
  styleUrls: ['./register-organizer.component.css']
})
export class RegisterOrganizerComponent implements OnInit {
  registerForm!: FormGroup;

  currentStep = 1;
  message: string = '';
  isLoading = false;
  registeredEmail: string = '';
  serverResponse: any = null;

  profilePicturePreview: string | null = null;
  @ViewChild('profilePictureInput') profilePictureInputRef!: ElementRef<HTMLInputElement>;

  currentPassword = true;
  validatePassword = true;

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
        Validators.pattern(/^(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]).{8,}$/)
      ]],
      confirmPassword: ['', Validators.required],
      name: ['', [Validators.required, Validators.pattern(/^[a-zA-ZÀ-ỹ\s]+$/)]],
      phoneNumber: ['', [Validators.required, Validators.pattern(/^[0-9]{10,11}$/)]],
      bio: ['', Validators.required],
      profilePicture: ['']
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
      this.message = 'Email không hợp lệ!';
    }
  }

  onVerifyCode() {
    if (this.currentStep !== 2) return;
    const codeControl = this.registerForm.get('code');

    if (codeControl?.valid) {
      const code = codeControl.value;
      this.isLoading = true;
      this.message = 'Đang kiểm tra mã xác thực...';

      this.authService.verifyCode(this.registeredEmail, code).subscribe({
        next: res => {
          this.isLoading = false;
          this.serverResponse = res;
          if (res.error) {
            this.message = res.error;
          } else {
            this.message = res.message || 'Mã chính xác!';
            this.currentStep = 3;
          }
        },
        error: () => {
          this.isLoading = false;
          this.message = 'Xác minh thất bại!';
        }
      });
    } else {
      this.message = 'Mã xác thực không hợp lệ!';
    }
  }

  onRegister() {
    if (this.currentStep !== 3) return;
    const password = this.registerForm.get('password')?.value;
    const confirmPassword = this.registerForm.get('confirmPassword')?.value; 
    const name = this.registerForm.get('name')?.value;
    const phoneNumber = this.registerForm.get('phoneNumber')?.value;
    const bio = this.registerForm.get('bio')?.value;
    const profilePicture = this.registerForm.get('profilePicture')?.value;
    if (password !== confirmPassword) {
      this.message = 'Mật khẩu không trùng khớp!';
      return;
    }

    this.isLoading = true;
    this.message = 'Đang đăng ký...';

    this.authService.registerOrganizer(this.registeredEmail, name, bio, password, confirmPassword, phoneNumber, profilePicture).subscribe({
      next: (res: any) => {
        this.isLoading = false;
        this.serverResponse = res;
        this.message = res?.message || 'Đăng ký thành công!';
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

  triggerFileInput(inputType: string): void {
  if (inputType === 'profilePicture') {
    this.profilePictureInputRef.nativeElement.click();
  }
  }


  onImageSelected(event: Event, type: string): void {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];

  if (!file) return;

  if (!file.type.startsWith('image/')) {
    alert('Vui lòng chọn file hình ảnh!');
    return;
  }

  if (file.size > 5 * 1024 * 1024) {
    alert('Kích thước ảnh không được vượt quá 5MB!');
    return;
  }

  const reader = new FileReader();
  reader.onload = () => {
    const imageDataUrl = reader.result as string;

    if (type === 'profilePicture') {
      this.profilePicturePreview = imageDataUrl;
      this.registerForm.patchValue({ profilePicture: imageDataUrl });
    }

    // Nếu có thêm loại ảnh khác, xử lý tại đây
  };

  reader.readAsDataURL(file);
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
