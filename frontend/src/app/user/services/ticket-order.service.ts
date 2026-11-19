import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, CreateOrderRequest, CreateOrderResponse } from '../../core/models';

@Injectable({
  providedIn: 'root',
})
export class TicketOrderService {
  private orderData: any;
  private readonly apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  setOrder(data: any): void {
    sessionStorage.setItem('orderData', JSON.stringify(data));
    this.orderData = data;
  }

  getOrder(): any {
    if (this.orderData) return this.orderData;
    const data = sessionStorage.getItem('orderData');
    if (data) {
      this.orderData = JSON.parse(data);
      return this.orderData;
    }
    return null;
  }

  orderTickets(orderData: {
    eventId: number;
    tickets: { ticketId: number; quantity: number }[];
    discountCode?: string;
  }): Observable<ApiResponse<CreateOrderResponse>> {
    return this.http.post<ApiResponse<CreateOrderResponse>>(`${this.apiUrl}/orders`, orderData);
  }

  payOrder(orderData: CreateOrderRequest | any): Observable<ApiResponse<CreateOrderResponse>> {
    return this.http.post<ApiResponse<CreateOrderResponse>>(`${this.apiUrl}/orders`, orderData);
  }

  clearOrder(): void {
    sessionStorage.removeItem('orderData');
    this.orderData = null;
  }

  getTicketsByUserId(userId: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/users/ticket/${userId}`);
  }
}
