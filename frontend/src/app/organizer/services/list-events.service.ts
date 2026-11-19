import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class ListEventsService {
  private apiUrl = 'http://localhost:8080/api/events/by-organizer';
  private baseUrl = 'http://localhost:8080/api/events';

  constructor(private http: HttpClient) { }

  getEventsByOrganizer(): Observable<any>{
    return this.http.get<any>(this.apiUrl);
  }

  getEventById(id: string): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/${id}`);
  }

  updateEvent(eventId: number, data: any, logo?: File, background?: File): Observable<any> {
    const formData = new FormData();

    formData.append('data', new Blob([JSON.stringify(data)], { type: 'application/json' }));

    if (logo) {
      formData.append('logo', logo, logo.name);
    }
    if (background) {
      formData.append('background', background, background.name);
    }

    return this.http.put(`${this.baseUrl}/${eventId}`, formData, {
      headers: {
        Accept: 'application/json'
      }
    });
  }
}