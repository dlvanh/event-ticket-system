import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class CreateEventsService {
  private readonly apiUrl = `${environment.apiUrl}/events`;

  constructor(private http: HttpClient) {}

  createEvent(eventData: any, logo: File, background: File): Observable<any> {
    const formData = new FormData();
    formData.append('data', new Blob([JSON.stringify(eventData)], { type: 'application/json' }));

    if (logo) {
      formData.append('logo', logo);
    }
    if (background) {
      formData.append('background', background);
    }

    return this.http.post(this.apiUrl, formData);
  }
}
