import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../../core/models';

export interface Location {
  id: number;
  name: string;
}

@Injectable({
  providedIn: 'root'
})
export class LocationService {
  private readonly apiUrl = environment.apiUrl;

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