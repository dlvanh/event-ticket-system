import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class CreateEventsService {
  private apiUrl = 'http://localhost:8080/api/events';

  constructor(private http: HttpClient) {}

  createEvent(
    eventData: any,
    logo: File,
    background: File
  ): Observable<any> {
    const formData = new FormData();

    // Gửi phần dữ liệu JSON
    formData.append('data', new Blob(
      [JSON.stringify(eventData)],
      { type: 'application/json' }
    ));

    // Gửi các file ảnh
    if (logo) {
      formData.append('logo', logo);
    }
    if (background) {
      formData.append('background', background);
    }

    // Gửi đến API
    return this.http.post(this.apiUrl, formData);
  }
}
