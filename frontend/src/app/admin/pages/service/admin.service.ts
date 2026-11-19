import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable,forkJoin } from 'rxjs';
import { map } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private apiUrl = 'http://localhost:8080/api/users';
  private eventsApiUrl = 'http://localhost:8080/api/events';
  private ordersApiUrl = 'http://localhost:8080/api/orders';

  private reviewsApiUrl = 'http://localhost:8080/api/review'; // Thêm biến cho review
  constructor(private http: HttpClient) {}

  // ==================== USER METHODS ====================

  // Lấy danh sách người dùng
   getUsers(): Observable<any> {
    return this.http.get<any>(this.apiUrl, { withCredentials: true }).pipe(
      map(response => {
        console.log('Raw API response:', JSON.stringify(response, null, 2));
        if (response && response.data && response.data.listUsers) {
          response.data.listUsers = response.data.listUsers.map((user: any) => ({
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

  getUserById(userId: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/${userId}`, { withCredentials: true }).pipe(
      map(user => ({
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
      }))
    );
  }
  // Lấy số lượng người dùng
  getUserCount(): Observable<number> {
    return this.http.get<any>(this.apiUrl).pipe(
      map(response => {
        // Xử lý response theo cấu trúc API của bạn
        if (response && response.data && response.data.listUsers && Array.isArray(response.data.listUsers)) {
          return response.data.listUsers.length;
        }
        return 0;
      })
    );
  }

  // Xóa người dùng
  deleteUsers(userIds: number[]): Observable<any> {
    const headers = new HttpHeaders({
      'Content-Type': 'application/json'
    });

    const body = {
      ids: userIds
    };

    return this.http.delete<any>(`${this.apiUrl}/delete`, {
      headers: headers,
      body: body
    });
  }

  // Xóa một người dùng (helper method)
  deleteUser(userId: number): Observable<any> {
    return this.deleteUsers([userId]);
  }

  // Vô hiệu hóa người dùng
  disableUsers(userIds: number[]): Observable<any> {
    const headers = new HttpHeaders({
      'Content-Type': 'application/json'
    });

    const body = {
      ids: userIds
    };

    return this.http.delete<any>(`${this.apiUrl}/disable`, {
      headers: headers,
      body: body
    });
  }

  // Vô hiệu hóa một người dùng (helper method)
  disableUser(userId: number): Observable<any> {
    return this.disableUsers([userId]);
  }
  // Lấy đánh giá theo userId
  getReviewsByUserId(userId: number): Observable<any> {
    return this.http.get<any>(`${this.reviewsApiUrl}/user/${userId}`, { withCredentials: true });
  }

  // Phê duyệt người tổ chức
// Phê duyệt người tổ chức
approveOrganizer(userId: number): Observable<any> {
  const headers = new HttpHeaders({
    'Content-Type': 'application/json'
  });

  return this.http.post<any>(`${this.apiUrl}/admin/approve-organizer?userId=${userId}`, null, {
    headers,
    withCredentials: true
  });
}
  // ==================== EVENT METHODS ====================

  // Lấy danh sách tất cả sự kiện
  getEvents(): Observable<any> {
    return this.http.get<any>(this.eventsApiUrl);
  }

  // Lấy danh sách sự kiện theo trạng thái
 // Trong file admin.service.ts

getEventsByStatus(status: string): Observable<any> {
  return this.http.get<any>(`${this.eventsApiUrl}?approvalStatus=${status}`);
}
  // Lấy danh sách sự kiện cần duyệt
  getPendingEvents(): Observable<any> {
    return this.getEventsByStatus('pending');
  }

  // Lấy danh sách sự kiện đã duyệt
  getApprovedEvents(): Observable<any> {
    return this.getEventsByStatus('approved');
  }

  // Lấy danh sách sự kiện bị từ chối
  getRejectedEvents(): Observable<any> {
    return this.getEventsByStatus('rejected');
  }

  // Lấy thông tin chi tiết sự kiện
  getEventById(eventId: number): Observable<any> {
    return this.http.get<any>(`${this.eventsApiUrl}/${eventId}`);
  }

  // Lấy số lượng sự kiện
  getEventCount(): Observable<number> {
    return this.http.get<any>(this.eventsApiUrl).pipe(
      map(response => {
        // Xử lý response theo cấu trúc API của bạn
        if (response && response.data && response.data.listEvents && Array.isArray(response.data.listEvents)) {
          return response.data.listEvents.length;
        }
        return 0;
      })
    );
  }

  // Lấy số lượng sự kiện theo trạng thái
  getEventCountByStatus(status: string): Observable<number> {
    return this.getEventsByStatus(status).pipe(
      map(response => {
        if (response && response.data && response.data.listEvents && Array.isArray(response.data.listEvents)) {
          return response.data.listEvents.length;
        }
        return 0;
      })
    );
  }

  // Duyệt sự kiện
  approveEvent(eventId: number): Observable<any> {
    const headers = new HttpHeaders({
      'Content-Type': 'application/json'
      // Authorization header sẽ được thêm bởi HTTP Interceptor
    });

    return this.http.put<any>(
      `${this.apiUrl}/admin/approve-event?eventId=${eventId}`,
      {},
      { headers }
    );
  }

  // Duyệt nhiều sự kiện
  approveEvents(eventIds: number[]): Observable<any> {
    const headers = new HttpHeaders({
      'Content-Type': 'application/json'
      // Authorization header sẽ được thêm bởi HTTP Interceptor
    });

    // Giả định API hỗ trợ duyệt hàng loạt với body chứa danh sách ids
    const body = {
      ids: eventIds
    };

    return this.http.put<any>(`${this.apiUrl}/admin/approve-event`, body, { headers });
  }

  // Từ chối sự kiện
  rejectEvent(eventId: number, reason: string): Observable<any> {
    const headers = new HttpHeaders({
      'Content-Type': 'application/json'
      // Authorization header được thêm bởi HTTP Interceptor
    });

    const body = {
      rejectionReason: reason || 'Không đạt yêu cầu'
    };

    return this.http.put<any>(
      `${this.apiUrl}/admin/reject-event?eventId=${eventId}`,
      body,
      { headers }
    );
  }

  // Từ chối nhiều sự kiện
  rejectEvents(eventIds: number[], reason: string): Observable<any> {
    const headers = new HttpHeaders({
      'Content-Type': 'application/json'
      // Authorization header được thêm bởi HTTP Interceptor
    });

    // Giả định API không hỗ trợ từ chối hàng loạt, gọi rejectEvent cho từng id
    const rejectObservables = eventIds.map(id => this.rejectEvent(id, reason));
    return forkJoin(rejectObservables);


  }

  // Thu hồi duyệt sự kiện (đưa về trạng thái pending)
  revokeApproval(eventId: number): Observable<any> {
    const headers = new HttpHeaders({
      'Content-Type': 'application/json'
    });

    return this.http.put<any>(`${this.eventsApiUrl}/${eventId}/revoke`, {}, { headers });
  }

  // Thu hồi duyệt nhiều sự kiện
  revokeApprovals(eventIds: number[]): Observable<any> {
    const headers = new HttpHeaders({
      'Content-Type': 'application/json'
    });

    const body = {
      ids: eventIds
    };

    return this.http.put<any>(`${this.eventsApiUrl}/revoke`, body, { headers });
  }

  // Xóa sự kiện
  deleteEvent(eventId: number): Observable<any> {
    return this.http.delete<any>(`${this.eventsApiUrl}/${eventId}`);
  }

  // Xóa nhiều sự kiện
  deleteEvents(eventIds: number[]): Observable<any> {
    const headers = new HttpHeaders({
      'Content-Type': 'application/json'
    });

    const body = {
      ids: eventIds
    };

    return this.http.delete<any>(`${this.eventsApiUrl}/delete`, {
      headers: headers,
      body: body
    });
  }

  // Tìm kiếm sự kiện
  searchEvents(keyword: string): Observable<any> {
    return this.http.get<any>(`${this.eventsApiUrl}/search?keyword=${encodeURIComponent(keyword)}`);
  }

  // Lấy sự kiện theo danh mục
  getEventsByCategory(category: string): Observable<any> {
    return this.http.get<any>(`${this.eventsApiUrl}?category=${encodeURIComponent(category)}`);
  }

  // Lấy sự kiện theo người tổ chức
  getEventsByOrganizer(organizerId: number): Observable<any> {
    return this.http.get<any>(`${this.eventsApiUrl}?organizer=${organizerId}`);
  }

  // Lấy lý do từ chối sự kiện
  getRejectionReason(eventId: number): Observable<any> {
    return this.http.get<any>(`${this.eventsApiUrl}/${eventId}/rejection-reason`);
  }

  exportEventExcel(eventId: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/events/${eventId}/report/excel`, { responseType: 'blob' });
  }

  exportEventPdf(eventId: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/events/${eventId}/report/pdf`, { responseType: 'blob' });
  }
   // Lấy danh sách đơn hàng

  getOrders(params: {
    status?: string,
    startAmount?: number,
    endAmount?: number,
    startTime?: string,
    endTime?: string,
    page?: number,
    size?: number
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
    queryParams = queryParams.set('page', (params.page || 0).toString());
    queryParams = queryParams.set('size', (params.size || 10).toString());

    return this.http.get<any>(`${this.ordersApiUrl}/list`, { params: queryParams });
  }

  getOrderCountByStatus(status: string): Observable<number> {
    return this.getOrders({ status }).pipe(
      map(response => {
        if (response && response.data && response.data.listOrders && Array.isArray(response.data.listOrders)) {
          return response.data.totalItems || response.data.listOrders.length;
        }
        return 0;
      })
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
