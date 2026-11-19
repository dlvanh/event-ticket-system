import { Injectable } from '@angular/core';
import { Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient} from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class TicketOrderService {
  private orderData: any;
  private apiUrl = 'http://localhost:8080/api';

  constructor(@Inject(PLATFORM_ID) private platformId: Object,
  private http: HttpClient) {}

  setOrder(data: any) {
    if (isPlatformBrowser(this.platformId)) {
      sessionStorage.setItem('orderData', JSON.stringify(data));
    }
    this.orderData = data;
  }

  getOrder(): any {
    if (this.orderData) return this.orderData;

    if (isPlatformBrowser(this.platformId)) {
      const data = sessionStorage.getItem('orderData');
      if (data) {
        this.orderData = JSON.parse(data);
        return this.orderData;
      }
    }

    return null;
  }

  orderTickets(orderData: {
    eventId: number;
    tickets: { ticketId: number; quantity: number }[];
    discountCode?: string;
  }): Observable<any> {
    return this.http.post(`${this.apiUrl}/orders`, orderData);
  }

  payOrder(orderData: any): Observable<any> {
    return this.http.post<any>('http://localhost:8080/api/orders', orderData);
  }

  clearOrder() {
    if (isPlatformBrowser(this.platformId)) {
      sessionStorage.removeItem('orderData');
    }
    this.orderData = null;
  }

  getTicketsByUserId(userId: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/users/ticket/${userId}`);
  }
}
