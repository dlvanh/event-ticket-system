import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ListEventsService {
  private readonly baseUrl = `${environment.apiUrl}/events`;

  constructor(private http: HttpClient) {}

  getEventsByOrganizer(): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/by-organizer`);
  }

  getEventById(id: string | number): Observable<any> {
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
      headers: { Accept: 'application/json' }
    });
  }

  exportEventReport(eventId: number, type: 'pdf' | 'excel'): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/${eventId}/report/${type}`, { responseType: 'blob' });
  }

  exportBuyerReport(eventId: number, type: 'pdf' | 'excel'): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/${eventId}/report/buyer-${type}`, { responseType: 'blob' });
  }
}