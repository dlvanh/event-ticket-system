import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable, forkJoin } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../../environments/environment';
import { UserProfile } from '../../../core/models';

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private readonly apiUrl = `${environment.apiUrl}/users`;
  private readonly eventsApiUrl = `${environment.apiUrl}/events`;
  private readonly ordersApiUrl = `${environment.apiUrl}/orders`;
  private readonly reviewsApiUrl = `${environment.apiUrl}/review`;

  constructor(private http: HttpClient) {}

  // ==================== USER METHODS ====================

  getUsers(): Observable<any> {
    return this.http.get<any>(this.apiUrl).pipe(
      map(response => {
        if (response?.data?.listUsers) {
          response.data.listUsers = response.data.listUsers.map((user: any): UserProfile => ({
            id: user.id,
            fullName: user.fullName,
            email: user.email,
            phoneNumber: user.phoneNumber,
            gender: user.gender,
            address: user.address,
            role: user.role,
            status: user.status,
            createdAt: user.createdAt,
            updatedAt: user.updatedAt,
            avatarUrl: user.avatarUrl,
            bio: user.bio
          }));
        }
        return response;
      })
    );
  }

  getUserById(userId: number): Observable<UserProfile> {
    return this.http.get<any>(`${this.apiUrl}/${userId}`).pipe(
      map(response => {
        const user = response?.data ?? response;
        return {
          id: user.id,
          fullName: user.fullName,
          email: user.email,
          phoneNumber: user.phoneNumber,
          gender: user.gender,
          address: user.address,
          role: user.role,
          status: user.status,
          createdAt: user.createdAt,
          updatedAt: user.updatedAt,
          avatarUrl: user.avatarUrl,
          bio: user.bio
        };
      })
    );
  }

  getUserCount(): Observable<number> {
    return this.http.get<any>(this.apiUrl).pipe(
      map(response => response?.data?.totalElements ?? response?.data?.listUsers?.length ?? 0)
    );
  }

  deleteUsers(userIds: number[]): Observable<any> {
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
    return this.http.delete<any>(`${this.apiUrl}/delete`, {
      headers,
      body: { ids: userIds }
    });
  }

  deleteUser(userId: number): Observable<any> {
    return this.deleteUsers([userId]);
  }

  disableUsers(userIds: number[]): Observable<any> {
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
    return this.http.delete<any>(`${this.apiUrl}/disable`, {
      headers,
      body: { ids: userIds }
    });
  }

  disableUser(userId: number): Observable<any> {
    return this.disableUsers([userId]);
  }

  getReviewsByUserId(userId: number): Observable<any> {
    return this.http.get<any>(`${this.reviewsApiUrl}/user/${userId}`);
  }

  approveOrganizer(userId: number): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/admin/approve-organizer?userId=${userId}`, null);
  }

  // ==================== EVENT METHODS ====================

  getEvents(): Observable<any> {
    return this.http.get<any>(this.eventsApiUrl);
  }

  getEventsByStatus(status: string): Observable<any> {
    return this.http.get<any>(`${this.eventsApiUrl}?approvalStatus=${encodeURIComponent(status)}`);
  }

  getPendingEvents(): Observable<any> {
    return this.getEventsByStatus('pending');
  }

  getApprovedEvents(): Observable<any> {
    return this.getEventsByStatus('approved');
  }

  getRejectedEvents(): Observable<any> {
    return this.getEventsByStatus('rejected');
  }

  getEventById(eventId: number): Observable<any> {
    return this.http.get<any>(`${this.eventsApiUrl}/${eventId}`);
  }

  getEventCount(): Observable<number> {
    return this.http.get<any>(this.eventsApiUrl).pipe(
      map(response => response?.data?.totalElements ?? response?.data?.listEvents?.length ?? 0)
    );
  }

  getEventCountByStatus(status: string): Observable<number> {
    return this.getEventsByStatus(status).pipe(
      map(response => response?.data?.totalElements ?? response?.data?.listEvents?.length ?? 0)
    );
  }

  approveEvent(eventId: number): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/admin/approve-event?eventId=${eventId}`, {});
  }

  approveEvents(eventIds: number[]): Observable<any> {
    return forkJoin(eventIds.map(id => this.approveEvent(id)));
  }

  rejectEvent(eventId: number, reason: string): Observable<any> {
    const body = { rejectionReason: reason || 'Không đạt yêu cầu' };
    return this.http.put<any>(`${this.apiUrl}/admin/reject-event?eventId=${eventId}`, body);
  }

  rejectEvents(eventIds: number[], reason: string): Observable<any> {
    return forkJoin(eventIds.map(id => this.rejectEvent(id, reason)));
  }

  exportEventExcel(eventId: number): Observable<Blob> {
    return this.http.get(`${this.eventsApiUrl}/${eventId}/report/excel`, { responseType: 'blob' });
  }

  exportEventPdf(eventId: number): Observable<Blob> {
    return this.http.get(`${this.eventsApiUrl}/${eventId}/report/pdf`, { responseType: 'blob' });
  }

  // ==================== ORDER METHODS ====================

  getOrders(params: {
    status?: string;
    startAmount?: number;
    endAmount?: number;
    startTime?: string;
    endTime?: string;
    page?: number;
    size?: number;
  }): Observable<any> {
    let queryParams = new HttpParams();
    if (params.status) queryParams = queryParams.set('status', params.status);
    if (params.startAmount !== undefined && params.startAmount !== null) {
      queryParams = queryParams.set('startAmount', params.startAmount.toString());
    }
    if (params.endAmount !== undefined && params.endAmount !== null) {
      queryParams = queryParams.set('endAmount', params.endAmount.toString());
    }
    if (params.startTime) queryParams = queryParams.set('startTime', params.startTime);
    if (params.endTime) queryParams = queryParams.set('endTime', params.endTime);
    queryParams = queryParams.set('page', (params.page ?? 1).toString());
    queryParams = queryParams.set('size', (params.size ?? 10).toString());

    return this.http.get<any>(`${this.ordersApiUrl}/list`, { params: queryParams });
  }

  getOrderCountByStatus(status: string): Observable<number> {
    return this.getOrders({ status, page: 1, size: 1 }).pipe(
      map(response => response?.data?.totalElements ?? response?.data?.listOrders?.length ?? 0)
    );
  }

  getOrderDetail(orderPayOSCode: number): Observable<any> {
    return this.http.get<any>(`${this.ordersApiUrl}/${orderPayOSCode}`);
  }

  cancelOrder(orderPayOSCode: number, cancellationReason: string): Observable<any> {
    const body = { cancellationReason: cancellationReason || 'Đã hủy bởi quản trị viên' };
    return this.http.put<any>(`${this.ordersApiUrl}/${orderPayOSCode}`, body);
  }
}
