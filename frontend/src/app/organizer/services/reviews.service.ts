import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ReviewsService {
  private readonly baseUrl = `${environment.apiUrl}/review/event`;

  constructor(private http: HttpClient) {}

  getReviewsByEventId(eventId: number | string): Observable<any> {
    return this.http.get(`${this.baseUrl}/${eventId}`);
  }
}
