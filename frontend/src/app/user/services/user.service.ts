import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, tap, EMPTY, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { jwtDecode } from 'jwt-decode';
import { Router } from '@angular/router';
import { isPlatformBrowser } from '@angular/common';
import { AuthService } from '../../auth/services/auth.service';

@Injectable({
  providedIn: 'root'
})
export class UserService {

  private apiUrl = 'http://localhost:8080/api/users';
  private reviewUrl = 'http://localhost:8080/api/review';

  constructor( private http: HttpClient,
    private router: Router,
    private authService: AuthService,
  @Inject(PLATFORM_ID) private platformId: Object) { }


  // Helper method để kiểm tra có phải browser không
  private isBrowser(): boolean {
    return isPlatformBrowser(this.platformId);
  }

  // Helper method để truy cập localStorage an toàn
  private getLocalStorage(): Storage | null {
    return this.isBrowser() ? localStorage : null;
  }

  /** Lấy thông tin người dùng */
  getUserInfo(): Observable<any> {
    return this.http.get(`${this.apiUrl}/current-profile`);
  }

  getToken(): string | null {
    if (!this.isBrowser()) return null;
    return localStorage.getItem('token'); 
  } 

  isUserLoggedIn(): boolean {
    return this.authService.isLoggedIn();
  }

  /** Lấy ảnh đại diện của người dùng */
  getAvatar(): Observable<string> {
    return this.http.get<{ data: string; message: string; status: number }>(
        `${this.apiUrl}/profile-picture`,
    ).pipe(
        map(response => response.data),
        catchError(error => {
            console.error('Avatar API error details:', {
                status: error.status,
                message: error.message,
                error: error.error
            });
            return of('assets/default-avatar.png');
        })
    );
  }


  updateUserProfile(profileData: any): Observable<any> {
    return this.http.put(`${this.apiUrl}/update-profile`, profileData);
  }

  // Lấy fullname kiểm tra người dùng đã đăng nhập hay chưa
  getFullName(): string | null {
    if (!this.isBrowser()) return null;
    return localStorage.getItem('fullName');
  }

  uploadAvatar(file: File): Observable<any> {
    const formData = new FormData();
    formData.append('file', file); // key là 'file'

    return this.http.post(`${this.apiUrl}/upload-profile-picture`, formData);
  }

  uploadReview(eventId: number, reviewData: any): Observable<any> {
    return this.http.post(`${this.reviewUrl}/upload/${eventId}`, reviewData);
  }

  updateReview(reviewId: number, reviewData: { rating: number; comment: string }): Observable<any> {
    return this.http.put(`${this.reviewUrl}/update/${reviewId}`, reviewData);
  }

  deleteReview(reviewId: number): Observable<any> {
    return this.http.delete(`${this.reviewUrl}/delete/${reviewId}`);
  }

  getReviewsByEvent(eventId: number): Observable<any> {
    return this.http.get(`${this.reviewUrl}/event/${eventId}`);
  }

  getDiscountByCode(code: string) {
    return this.http.get<any>(`http://localhost:8080/api/discounts/${code}`);
  }
}
