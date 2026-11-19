import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { AuthService } from '../../auth/services/auth.service';
import { environment } from '../../../environments/environment';
import { ApiResponse, DiscountDto } from '../../core/models';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private readonly apiUrl = `${environment.apiUrl}/users`;
  private readonly reviewUrl = `${environment.apiUrl}/review`;
  private readonly discountUrl = `${environment.apiUrl}/discounts`;

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  getUserInfo(): Observable<any> {
    return this.http.get(`${this.apiUrl}/current-profile`);
  }

  getToken(): string | null {
    return this.authService.getToken();
  }

  isUserLoggedIn(): boolean {
    return this.authService.isLoggedIn();
  }

  getAvatar(): Observable<string> {
    return this.http.get<ApiResponse<string>>(`${this.apiUrl}/profile-picture`).pipe(
      map(response => response.data),
      catchError(() => of('assets/default-avatar.png'))
    );
  }

  updateUserProfile(profileData: any): Observable<any> {
    return this.http.put(`${this.apiUrl}/update-profile`, profileData);
  }

  getFullName(): string | null {
    return localStorage.getItem('fullName');
  }

  uploadAvatar(file: File): Observable<any> {
    const formData = new FormData();
    formData.append('file', file);
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

  getDiscountByCode(code: string): Observable<ApiResponse<DiscountDto>> {
    return this.http.get<ApiResponse<DiscountDto>>(`${this.discountUrl}/${encodeURIComponent(code)}`);
  }
}
