import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { jwtDecode } from 'jwt-decode';
import { Router } from '@angular/router';
import { environment } from '../../../environments/environment';
import { ApiResponse, DecodedJwtPayload, LoginResponseDto, OrganizerRegistrationPayload } from '../../core/models';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly apiUrl = environment.apiUrl;

  constructor(
    private http: HttpClient,
    private router: Router
  ) {}

  /** Gửi mã xác minh đến email */
  sendVerificationCode(email: string): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.apiUrl}/auth/sendVerificationCode`, { email });
  }

  /** Kiểm tra mã xác minh */
  verifyCode(email: string, code: string): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.apiUrl}/auth/verifyCode`, { email, code });
  }

  /** Đăng ký tài khoản mới */
  register(
    email: string,
    password: string,
    confirmPassword: string,
    fullName: string
  ): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.apiUrl}/auth/register`, {
      email,
      password,
      confirmPassword,
      fullName
    });
  }

  /** Đăng ký tài khoản nhà tổ chức (multipart/form-data) */
  registerOrganizer(
    payload: OrganizerRegistrationPayload,
    profilePicture?: File | null
  ): Observable<ApiResponse<void>> {
    const formData = new FormData();
    formData.append(
      'data',
      new Blob([JSON.stringify(payload)], { type: 'application/json' })
    );
    if (profilePicture) {
      formData.append('profilePicture', profilePicture, profilePicture.name);
    }
    return this.http.post<ApiResponse<void>>(`${this.apiUrl}/auth/register-organizer`, formData);
  }

  /** Đăng nhập */
  login(email: string, password: string): Observable<ApiResponse<LoginResponseDto>> {
    return this.http.post<ApiResponse<LoginResponseDto>>(`${this.apiUrl}/auth/login`, { email, password }).pipe(
      tap((res: any) => {
        const token: string | undefined = res?.data?.token ?? res?.token;
        if (token) {
          this.setToken(token);
          const decodedToken = this.decodeToken(token);
          if (decodedToken?.sub) {
            localStorage.setItem('userId', decodedToken.sub);
          }
          if (decodedToken?.full_name) {
            localStorage.setItem('fullName', decodedToken.full_name);
          }
        }
      })
    );
  }

  /** Đặt lại mật khẩu B1: gửi mã xác thực */
  sendCode(email: string): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.apiUrl}/auth/send-code`, { email });
  }

  /** Đặt lại mật khẩu B2: xác thực mã và đổi mật khẩu */
  resetPassword(payload: {
    email: string;
    code: string;
    newPassword: string;
    confirmNewPassword: string;
  }): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.apiUrl}/auth/reset-password-by-code`, payload);
  }

  getUserId(): number | null {
    const token = this.getToken();
    if (!token) return null;
    const decoded = this.decodeToken(token);
    return decoded?.sub ? Number(decoded.sub) : null;
  }

  setToken(token: string): void {
    localStorage.setItem('token', token);
    const decodedToken = this.decodeToken(token);
    if (decodedToken?.role) {
      localStorage.setItem('role', JSON.stringify(decodedToken.role));
    }
  }

  getToken(): string | null {
    return localStorage.getItem('token');
  }

  isLoggedIn(): boolean {
    const token = this.getToken();
    if (!token) return false;
    const decoded = this.decodeToken(token);
    if (!decoded) {
      this.logout();
      return false;
    }
    if (decoded.exp && Date.now() >= decoded.exp * 1000) {
      this.logout();
      return false;
    }
    return true;
  }

  logout(): void {
    localStorage.clear();
    sessionStorage.clear();
  }

  decodeToken(token: string): DecodedJwtPayload | null {
    if (!token) return null;
    try {
      return jwtDecode<DecodedJwtPayload>(token);
    } catch {
      return null;
    }
  }

  getRole(): string | null {
    const token = this.getToken();
    if (!token) return null;
    return this.decodeToken(token)?.role ?? null;
  }

  isAdmin(): boolean {
    const role = this.getRole();
    return role === 'ROLE_admin' || role === 'ROLE_ADMIN';
  }

  isSeller(): boolean {
    const role = this.getRole();
    return role === 'ROLE_organizer' || role === 'ROLE_ORGANIZER';
  }

  isCustomer(): boolean {
    const role = this.getRole();
    return role === 'ROLE_customer' || role === 'ROLE_CUSTOMER';
  }
}