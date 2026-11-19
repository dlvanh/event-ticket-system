import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';

export interface Location {
  id: number;
  name: string;
}

export interface ApiResponse<T> {
  data: T;
}

@Injectable({
  providedIn: 'root'
})
export class LocationService {
  private apiUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  getProvinces(): Observable<Location[]> {
    return this.http.get<ApiResponse<Location[]>>(`${this.apiUrl}/provinces`).pipe(
      map(response => response.data)
    );
  }

  getDistricts(provinceId: number): Observable<Location[]> {
    return this.http.get<ApiResponse<Location[]>>(`${this.apiUrl}/districts/${provinceId}`).pipe(
      map(response => response.data)
    );
  }

  getWards(districtId: number): Observable<Location[]> {
    return this.http.get<ApiResponse<Location[]>>(`${this.apiUrl}/wards/${districtId}`).pipe(
      map(response => response.data)
    );
  }
} 