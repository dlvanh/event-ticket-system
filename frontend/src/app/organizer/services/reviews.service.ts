import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ReviewsService {
  private baseUrl = 'http://localhost:8080/api/review/event';

  constructor(private http: HttpClient) {}

  getReviewsByEventId(eventId: number | string): Observable<any> {
    return this.http.get(`${this.baseUrl}/${eventId}`);
  }
}
