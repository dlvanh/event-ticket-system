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
  message = '';
  isLoading = false;
  registeredEmail = '';
  serverResponse: any = null;

  profilePicturePreview: string | null = null;
  selectedProfilePictureFile: File | null = null;
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
        Validators.pattern(/^(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]).{10,}$/)
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
      const code = codeControl.value;
      this.isLoading = true;
      this.message = 'Đang kiểm tra mã xác thực...';

      this.authService.verifyCode(this.registeredEmail, code).subscribe({
        next: (res) => {
          this.isLoading = false;
          this.serverResponse = res;
          this.message = res?.message || 'Mã chính xác!';
          this.currentStep = 3;
        },
        error: (err) => {
          this.isLoading = false;
          this.serverResponse = err;
          this.message = this.extractErrorMessage(err) || 'Xác minh thất bại!';
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

    if (password !== confirmPassword) {
      this.message = 'Mật khẩu không trùng khớp!';
      return;
    }

    this.isLoading = true;
    this.message = 'Đang đăng ký...';

    this.authService.registerOrganizer(
      {
        email: this.registeredEmail,
        name,
        bio,
        password,
        confirmPassword,
        phoneNumber
      },
      this.selectedProfilePictureFile
    ).subscribe({
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

    if (type === 'profilePicture') {
      this.selectedProfilePictureFile = file;
      const reader = new FileReader();
      reader.onload = () => {
        this.profilePicturePreview = reader.result as string;
        this.registerForm.patchValue({ profilePicture: file.name });
      };
      reader.readAsDataURL(file);
    }
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
