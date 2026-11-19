import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { jwtDecode } from 'jwt-decode';
import { Router } from '@angular/router';
import { isPlatformBrowser } from '@angular/common';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'http://localhost:8080/api';
  
  constructor(
    private http: HttpClient,
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object
  ) { }

  // Helper method để kiểm tra có phải browser không
  private isBrowser(): boolean {
    return isPlatformBrowser(this.platformId);
  }

  // Helper method để truy cập localStorage an toàn
  private getLocalStorage(): Storage | null {
    return this.isBrowser() ? localStorage : null;
  }

  /** Gửi mã xác minh đến email */
  sendVerificationCode(email: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/auth/sendVerificationCode`, { email }, { responseType: 'text' });
  }

  /** Kiểm tra mã xác minh */
  verifyCode(email: string, code: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/auth/verifyCode`, { email, code }, { responseType: 'json' });
  }

  /** Đăng ký tài khoản mới */
  register(email: string, password: string, confirmPassword: string, fullName: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/auth/register`, { email, password, confirmPassword, fullName }, { responseType: 'text' });
  }

  registerOrganizer(
  email: string,
  name: string,
  bio: string,
  password: string,
  confirmPassword: string,
  phoneNumber: string,
  profilePicture: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/auth/register-organizer`, { email,name, bio, password, confirmPassword, phoneNumber, profilePicture}, { responseType: 'text' });
}


  /** Đăng nhập */
  login(email: string, password: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/auth/login`, { email, password }).pipe(
      tap((res: any) => {
        if (res.token && this.isBrowser()) {
          this.setToken(res.token);

          try {
            // Giải mã token để lấy thông tin user
            const decodedToken: any = jwtDecode(res.token);
            const storage = this.getLocalStorage();
            if (storage) {
              if (decodedToken.sub) {
                storage.setItem('userId', decodedToken.sub);
              }
              
              if (decodedToken.full_name) {
                storage.setItem('fullName',decodedToken.full_name);
              }
            }

          } catch (error) {
            console.error('Lỗi khi giải mã token:', error);
          }
        }
      })
    );
  }

  // Đặt lại mật khẩu B1 gửi code
  sendCode(email: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/auth/send-code`, { email }, { responseType: 'text' });
  }

  // Đặt lại mật khẩu B2 xác thực code và đổi mật khẩu
  resetPassword(payload: { email: string, code: string, newPassword: string, confirmNewPassword: string }): Observable<any> {
    return this.http.post(`${this.apiUrl}/auth/reset-password-by-code`, payload, { responseType: 'text' });
  }

  // Lấy userid
  getUserId(): number | null { 
    if (!isPlatformBrowser(this.platformId)) {
      return null;
    }

    const token = localStorage.getItem('token'); 
    if (!token) return null; 

    try { 
      const decodedToken: any = jwtDecode(token); 
      return Number(decodedToken.sub); // ID người dùng 
    } catch (error) { 
      console.error('Lỗi khi giải mã token:', error); 
      return null; 
    } 
  } 

  /*Lưu token vào localStorage */
  setToken(token: string): void { 
    
    localStorage.setItem('token', token); 
    const decodedToken = this.decodeToken(token); 
    if (decodedToken) { 
      localStorage.setItem('role', JSON.stringify(decodedToken.role)); // Lưu role vào localStorage 
    } 
  } 

  /*Lấy token */
  getToken(): string | null {
    return typeof window !== 'undefined' ? localStorage.getItem('token') : null;
  }


  /*Kiểm tra xem user có đăng nhập không */
  isLoggedIn(): boolean {
    if (!this.isBrowser()) return false;
    const token = this.getToken();
    if (!token) return false;

    try {
      const decoded: any = jwtDecode(token);
      // Kiểm tra hạn token (exp là giây)
      if (decoded.exp && Date.now() >= decoded.exp * 1000) {
        this.logout();
        return false;
      }
      return true;
    } catch {
      this.logout();
      return false;
    }
  }

  /*Đăng xuất */
  logout(): void {
    if (!this.isBrowser()) return; 
    localStorage.clear();
    // Clear sessionStorage if available
    if (typeof sessionStorage !== 'undefined') {
      sessionStorage.clear();
    }
  }

  /*Giải mã token */
  decodeToken(token: string): any {
    if (!token) return null;
    try {
      return jwtDecode(token);
    } catch (error) {
      console.error('Invalid token:', error);
      return null;
    }
  }

  /*Lấy role từ localStorage hoặc token */
  getRole(): string | null { 
    const token = this.getToken(); 
    if (!token) return null; 

    try { 
      const decodedToken: any = jwtDecode(token); 
      return decodedToken.role || null; // Lấy role từ JWT 
    } catch (error) { 
      console.error('Invalid token:', error); 
      return null; 
    } 
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