import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class EventsService {

  private apiUrl = 'http://localhost:8080/api/events';

  constructor(private http: HttpClient) { }

   getRecommendedEvents(
    category?: string,
    address?: string,
    startTime?: string,
    endTime?: string,
    name?: string,
    sortBy?: string,
    page: number = 1,
    size: number = 12
  ): Observable<any> {
    let params = new HttpParams();

    if (category) params = params.set('category', category);
    if (address) params = params.set('address', address);
    if (startTime) params = params.set('startTime', startTime);
    if (endTime) params = params.set('endTime', endTime);
    if (name) params = params.set('name', name);
    if (sortBy) params = params.set('sortBy', sortBy);
    
    params = params.set('page', page.toString());
    params = params.set('size', size.toString());

    return this.http.get(`${this.apiUrl}/recommend`, { params });
  }

  getEventById(eventId: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/${eventId}`);
  }

  searchEvents(params: { category?: string; address?: string, startTime?: string , endTime?: string , name?: string; page?: number;}): Observable<any> {
    let httpParams = new HttpParams();

    if (params.category) {
      httpParams = httpParams.set('category', params.category);
    }

    if (params.address) {
      httpParams = httpParams.set('address', params.address);
    }

    if (params.startTime) {
      httpParams = httpParams.set('startTime', params.startTime);
    }
    
    if (params.endTime) {
      httpParams = httpParams.set('endTime', params.endTime);
    }

    if (params.name) {                   
      httpParams = httpParams.set('name', params.name);
    }

    if (params.page !== undefined) {
      httpParams = httpParams.set('page', params.page.toString());
    }
    
    return this.http.get(`${this.apiUrl}/recommend`, { params: httpParams });
  }
}
