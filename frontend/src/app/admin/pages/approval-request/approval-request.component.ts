import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService } from '../service/admin.service';
import { HttpClient } from '@angular/common/http'; // Add HttpClient import
interface Event {
  id: number;
  title: string;
  description: string;
  startDate: string;
  endDate: string;
  startTime: string;
  endTime: string;
  location: string;
  category: string;
  status: 'pending' | 'approved' | 'rejected' | 'cancelled';
  createdDate: string;
  updatedDate?: string;
  imageUrl?: string; // logoUrl
  backgroundUrl?: string; // backgroundUrl
  organizer: {
    id: number;
    name: string;
    avatar?: string; // organizerAvatarUrl
    bio?: string; // organizerBio
    email?: string; // organizerEmail
    phoneNumber?: string | null; // organizerPhoneNumber
  };
  rejectionReason?: string;
  ticketPrices?: any;
  ticketsSold?: any;
}

@Component({
  selector: 'app-approval-request',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './approval-request.component.html',
  styleUrls: ['./approval-request.component.css']
})
export class ApprovalRequestComponent {
  events: Event[] = [];
  filteredEvents: Event[] = [];
  loading: boolean = false;
  searchKeyword: string = '';
  selectedStatus: string = 'all';
  selectedCategory: string = 'all';

  // Pagination
  currentPage: number = 1;
  itemsPerPage: number = 10;
  totalItems: number = 0;

  // Selection
  selectedEvents: number[] = [];
  selectAll: boolean = false;

  // Modal states
  showDetailModal: boolean = false;
  showRejectModal: boolean = false;
  showConfirmModal: boolean = false;
  selectedEvent: Event | null = null;
  rejectReason: string = '';
  confirmAction: string = '';

  // Statistics
  stats = {
    total: 0,
    pending: 0,
    approved: 0,
    rejected: 0
  };

  constructor(private adminService: AdminService, private http: HttpClient) {}

  ngOnInit() {
    this.loadEvents();
    this.loadStatistics();
  }

 // Load events
  loadEvents() {
    this.loading = true;
    this.adminService.getEvents().subscribe({
      next: (response) => {
        if (response && response.data && response.data.listEvents) {
          this.events = response.data.listEvents.map((apiEvent: any) => ({
            id: apiEvent.eventId,
            title: apiEvent.eventName,
            description: apiEvent.description || 'Không có mô tả',
            startDate: apiEvent.startTime ? apiEvent.startTime.split('T')[0] : '',
            endDate: apiEvent.endTime ? apiEvent.endTime.split('T')[0] : '',
            startTime: apiEvent.startTime ? apiEvent.startTime.split('T')[1] : '',
            endTime: apiEvent.endTime ? apiEvent.endTime.split('T')[1] : '',
            location: apiEvent.address || 'Chưa cung cấp địa chỉ',
            category: apiEvent.category || 'Không xác định',
            status: apiEvent.approvalStatus,
            createdDate: apiEvent.createdAt,
            updatedDate: apiEvent.updatedAt,
            imageUrl: apiEvent.logoUrl,
            backgroundUrl: apiEvent.backgroundUrl,
            organizer: {
              id: apiEvent.organizerId || 0,
              name: apiEvent.organizerName || 'Không xác định',
              avatar: apiEvent.organizerAvatarUrl,
              bio: apiEvent.organizerBio,
              email: apiEvent.organizerEmail,
              phoneNumber: apiEvent.organizerPhoneNumber
            },
            rejectionReason: apiEvent.rejectReason || ''
          }));
          console.log('Mapped events:', this.events);
          this.stats.total = this.events.length;
          this.stats.pending = this.events.filter(event => event.status === 'pending').length;
          this.stats.approved = this.events.filter(event => event.status === 'approved').length;
          this.stats.rejected = this.events.filter(event => event.status === 'rejected').length;
          console.log('Stats:', this.stats);
          this.applyFilters();
        }
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading events:', error);
        this.loading = false;
      }
    });
  }

  // Load statistics
  loadStatistics() {
    this.adminService.getEventCount().subscribe({
      next: (count) => (this.stats.total = count),
      error: (error) => console.error('Error loading total events:', error)
    });
    this.adminService.getEventCountByStatus('pending').subscribe({
      next: (count) => (this.stats.pending = count),
      error: (error) => console.error('Error loading pending events:', error)
    });
    this.adminService.getEventCountByStatus('approved').subscribe({
      next: (count) => (this.stats.approved = count),
      error: (error) => console.error('Error loading approved events:', error)
    });
    this.adminService.getEventCountByStatus('rejected').subscribe({
      next: (count) => (this.stats.rejected = count),
      error: (error) => console.error('Error loading rejected events:', error)
    });
  }

  // Export Excel for a single event
  exportEventExcel(eventId: number) {
    this.loading = true;
    const url = `http://localhost:8080/api/events/${eventId}/report/excel`;
    this.http.get(url, { responseType: 'blob' }).subscribe({
      next: (response: Blob) => {
        this.downloadFile(response, `event_${eventId}_report.xlsx`);
        this.showSuccess('Xuất file Excel thành công!');
        this.loading = false;
      },
      error: (error) => {
        console.error('Error exporting Excel:', error);
        this.showError('Có lỗi xảy ra khi xuất file Excel!');
        this.loading = false;
      }
    });
  }

  // Optional: Batch export Excel for selected events
  batchExportExcel() {
    if (this.selectedEvents.length === 0) {
      this.showError('Vui lòng chọn ít nhất một sự kiện để xuất!');
      return;
    }

    this.loading = true;
    const exportPromises = this.selectedEvents.map(eventId => {
      const url = `http://localhost:8080/api/events/${eventId}/report/excel`;
      return this.http.get(url, { responseType: 'blob' }).toPromise().then((response: Blob | undefined) => {
        if (response) {
          this.downloadFile(response, `event_${eventId}_report.xlsx`);
        }
      });
    });

    Promise.all(exportPromises)
      .then(() => {
        this.showSuccess(`Đã xuất ${this.selectedEvents.length} file Excel thành công!`);
        this.loading = false;
      })
      .catch(error => {
        console.error('Error batch exporting Excel:', error);
        this.showError('Có lỗi xảy ra khi xuất hàng loạt file Excel!');
        this.loading = false;
      });
  }

  // Utility method to download file
  private downloadFile(blob: Blob, fileName: string) {
    const link = document.createElement('a');
    link.href = window.URL.createObjectURL(blob);
    link.download = fileName;
    link.click();
    window.URL.revokeObjectURL(link.href);
  }
    // Export PDF for a single event
  exportEventPdf(eventId: number) {
    this.loading = true;
    const url = `http://localhost:8080/api/events/${eventId}/report/pdf`;
    this.http.get(url, { responseType: 'blob' }).subscribe({
      next: (response: Blob) => {
        this.downloadFile(response, `event_${eventId}_report.pdf`);
        this.showSuccess('Xuất file PDF thành công!');
        this.loading = false;
      },
      error: (error) => {
        console.error('Error exporting PDF:', error);
        this.showError('Có lỗi xảy ra khi xuất file PDF!');
        this.loading = false;
      }
    });
  }

  // Batch export PDF
  batchExportPdf() {
    if (this.selectedEvents.length === 0) {
      this.showError('Vui lòng chọn ít nhất một sự kiện để xuất!');
      return;
    }
    this.loading = true;
    const exportPromises = this.selectedEvents.map(eventId => {
      const url = `http://localhost:8080/api/events/${eventId}/report/pdf`;
      return this.http.get(url, { responseType: 'blob' }).toPromise().then((response: Blob | undefined) => {
        if (response) {
          this.downloadFile(response, `event_${eventId}_report.pdf`);
        }
      });
    });
    Promise.all(exportPromises)
      .then(() => {
        this.showSuccess(`Đã xuất ${this.selectedEvents.length} file PDF thành công!`);
        this.loading = false;
      })
      .catch(error => {
        console.error('Error batch exporting PDF:', error);
        this.showError('Có lỗi xảy ra khi xuất hàng loạt file PDF!');
        this.loading = false;
      });
  }
  // Apply filters
  applyFilters() {
    this.filteredEvents = this.events.filter(event => {
      const matchesSearch = !this.searchKeyword ||
        event.title.toLowerCase().includes(this.searchKeyword.toLowerCase()) ||
        event.organizer.name.toLowerCase().includes(this.searchKeyword.toLowerCase());

      const matchesStatus = this.selectedStatus === 'all' || event.status === this.selectedStatus;
      const matchesCategory = this.selectedCategory === 'all' || event.category === this.selectedCategory;

      return matchesSearch && matchesStatus && matchesCategory;
    });

    this.totalItems = this.filteredEvents.length;
    this.currentPage = 1;
  }

  // Search events
  onSearch() {
    this.applyFilters();
  }

  // Filter by status
  onStatusChange() {
    this.applyFilters();
  }

  // Filter by category
  onCategoryChange() {
    this.applyFilters();
  }

  // Get paginated events
  getPaginatedEvents(): Event[] {
    const startIndex = (this.currentPage - 1) * this.itemsPerPage;
    const endIndex = startIndex + this.itemsPerPage;
    return this.filteredEvents.slice(startIndex, endIndex);
  }

  // Pagination methods
  getTotalPages(): number {
    return Math.ceil(this.totalItems / this.itemsPerPage);
  }

  goToPage(page: number) {
    if (page >= 1 && page <= this.getTotalPages()) {
      this.currentPage = page;
    }
  }

  getDisplayEndIndex(): number {
    return Math.min(this.currentPage * this.itemsPerPage, this.totalItems);
  }

  // Selection methods
  toggleEventSelection(eventId: number) {
    const index = this.selectedEvents.indexOf(eventId);
    if (index > -1) {
      this.selectedEvents.splice(index, 1);
    } else {
      this.selectedEvents.push(eventId);
    }
    this.updateSelectAll();
  }

  toggleSelectAll() {
    if (this.selectAll) {
      this.selectedEvents = this.getPaginatedEvents().map(event => event.id);
    } else {
      this.selectedEvents = [];
    }
  }

  updateSelectAll() {
    const paginatedEvents = this.getPaginatedEvents();
    this.selectAll = paginatedEvents.length > 0 &&
      paginatedEvents.every(event => this.selectedEvents.includes(event.id));
  }

  isEventSelected(eventId: number): boolean {
    return this.selectedEvents.includes(eventId);
  }

  // Event actions
  approveEvent(eventId: number) {
    this.adminService.approveEvent(eventId).subscribe({
      next: (response) => {
        this.showSuccess('Sự kiện đã được duyệt thành công!');
        this.loadEvents();
        this.loadStatistics();
      },
      error: (error) => {
        console.error('Error approving event:', error);
        this.showError('Có lỗi xảy ra khi duyệt sự kiện!');
      }
    });
  }

  rejectEvent(eventId: number, reason: string) {
    this.adminService.rejectEvent(eventId, reason).subscribe({
      next: () => {
        this.showSuccess('Sự kiện đã bị từ chối!');
        this.loadEvents();
        this.loadStatistics();
        this.showRejectModal = false;
        this.rejectReason = '';
      },
      error: (error) => {
        console.error('Error rejecting event:', error);
        this.showError('Có lỗi xảy ra khi từ chối sự kiện!');
      }
    });
  }

  revokeApproval(eventId: number) {
    this.adminService.revokeApproval(eventId).subscribe({
      next: () => {
        this.showSuccess('Đã thu hồi duyệt sự kiện!');
        this.loadEvents();
        this.loadStatistics();
      },
      error: (error) => {
        console.error('Error revoking approval:', error);
        this.showError('Có lỗi xảy ra khi thu hồi duyệt!');
      }
    });
  }

  deleteEvent(eventId: number) {
    this.adminService.deleteEvent(eventId).subscribe({
      next: () => {
        this.showSuccess('Sự kiện đã được xóa!');
        this.loadEvents();
        this.loadStatistics();
      },
      error: (error) => {
        console.error('Error deleting event:', error);
        this.showError('Có lỗi xảy ra khi xóa sự kiện!');
      }
    });
  }

  // Batch actions
  batchApprove() {
    if (this.selectedEvents.length === 0) return;

    this.adminService.approveEvents(this.selectedEvents).subscribe({
      next: () => {
        this.showSuccess(`Đã duyệt ${this.selectedEvents.length} sự kiện!`);
        this.selectedEvents = [];
        this.selectAll = false;
        this.loadEvents();
        this.loadStatistics();
      },
      error: (error) => {
        console.error('Error approving events:', error);
        this.showError('Có lỗi xảy ra khi duyệt hàng loạt!');
      }
    });
  }

  batchReject() {
    if (this.selectedEvents.length === 0) return;

    this.adminService.rejectEvents(this.selectedEvents, this.rejectReason).subscribe({
      next: () => {
        this.showSuccess(`Đã từ chối ${this.selectedEvents.length} sự kiện!`);
        this.selectedEvents = [];
        this.selectAll = false;
        this.showRejectModal = false;
        this.rejectReason = '';
        this.loadEvents();
        this.loadStatistics();
      },
      error: (error) => {
        console.error('Error rejecting events:', error);
        this.showError('Có lỗi xảy ra khi từ chối hàng loạt!');
      }
    });
  }

  batchDelete() {
    if (this.selectedEvents.length === 0) return;

    this.adminService.deleteEvents(this.selectedEvents).subscribe({
      next: () => {
        this.showSuccess(`Đã xóa ${this.selectedEvents.length} sự kiện!`);
        this.selectedEvents = [];
        this.selectAll = false;
        this.loadEvents();
        this.loadStatistics();
      },
      error: (error) => {
        console.error('Error deleting events:', error);
        this.showError('Có lỗi xảy ra khi xóa hàng loạt!');
      }
    });
  }

  // Modal methods
  // View event detail
  viewEventDetail(event: Event) {
    this.loading = true;
    this.adminService.getEventById(event.id).subscribe({
      next: (response) => {
        if (response && response.data) {
          const apiEvent = response.data;
          this.selectedEvent = {
            id: apiEvent.eventId,
            title: apiEvent.eventName,
            description: apiEvent.description || 'Không có mô tả',
            startDate: apiEvent.startTime ? apiEvent.startTime.split('T')[0] : '',
            endDate: apiEvent.endTime ? apiEvent.endTime.split('T')[0] : '',
            startTime: apiEvent.startTime ? apiEvent.startTime.split('T')[1] : '',
            endTime: apiEvent.endTime ? apiEvent.endTime.split('T')[1] : '',
            location: apiEvent.address || 'Chưa cung cấp địa chỉ',
            category: apiEvent.category || 'Không xác định',
            status: apiEvent.approvalStatus,
            createdDate: apiEvent.createdAt,
            updatedDate: apiEvent.updatedAt,
            imageUrl: apiEvent.logoUrl || 'https://via.placeholder.com/300x200/007bff/ffffff?text=Event',
            backgroundUrl: apiEvent.backgroundUrl || 'https://via.placeholder.com/300x200/007bff/ffffff?text=Background',
            organizer: {
              id: apiEvent.organizerId || 0,
              name: apiEvent.organizerName || 'Không xác định',
              avatar: apiEvent.organizerAvatarUrl || 'https://via.placeholder.com/32x32/28a745/ffffff?text=User',
              bio: apiEvent.organizerBio || 'Chưa có thông tin',
              email: apiEvent.organizerEmail || 'Chưa cung cấp',
              phoneNumber: apiEvent.organizerPhoneNumber || 'Chưa cung cấp'
            },
            rejectionReason: apiEvent.rejectReason || '',
            ticketPrices: apiEvent.ticketPrices || {},
            ticketsSold: apiEvent.ticketsSold || {}
          };
          this.showDetailModal = true;
        }
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading event detail:', error);
        this.showError('Có lỗi xảy ra khi tải chi tiết sự kiện!');
        this.loading = false;
      }
    });
  }
goToEventDetail(eventId: number) {
  // Điều hướng hoặc xử lý logic khi click vào ảnh
  console.log('Navigate to event detail:', eventId);
  // Ví dụ: this.router.navigate(['/event', eventId]);
}
  openRejectModal(event: Event) {
    this.selectedEvent = event;
    this.showRejectModal = true;
  }

  openConfirmModal(action: string, event?: Event) {
    this.confirmAction = action;
    this.selectedEvent = event || null;
    this.showConfirmModal = true;
  }

  confirmModalAction() {
    if (!this.selectedEvent) return;

    switch (this.confirmAction) {
      case 'approve':
        this.approveEvent(this.selectedEvent.id);
        break;
      case 'revoke':
        this.revokeApproval(this.selectedEvent.id);
        break;
      case 'delete':
        this.deleteEvent(this.selectedEvent.id);
        break;
    }

    this.showConfirmModal = false;
  }

  closeModal() {
    this.showDetailModal = false;
    this.showRejectModal = false;
    this.showConfirmModal = false;
    this.selectedEvent = null;
    this.rejectReason = '';
  }

  // Utility methods
  getStatusClass(status: string): string {
    switch (status) {
      case 'pending': return 'badge-warning';
      case 'approved': return 'badge-success';
      case 'rejected': return 'badge-danger';
      default: return 'badge-secondary';
    }
  }

  getStatusText(status: string): string {
    switch (status) {
      case 'pending': return 'Chờ duyệt';
      case 'approved': return 'Đã duyệt';
      case 'rejected': return 'Từ chối';
      default: return 'Không xác định';
    }
  }

  getStatusIcon(status: string): string {
    switch (status) {
      case 'pending': return 'fas fa-clock';
      case 'approved': return 'fas fa-check';
      case 'rejected': return 'fas fa-times';
      default: return 'fas fa-question';
    }
  }

  formatDate(dateString: string): string {
    if (!dateString) return 'Không xác định';
    const date = new Date(dateString);
    return date.toLocaleDateString('vi-VN');
  }

  formatDateTime(dateString: string): string {
    if (!dateString) return 'Không xác định';
    const date = new Date(dateString);
    return date.toLocaleString('vi-VN');
  }

  // Toast notifications
  showSuccess(message: string) {
    console.log('Success:', message);
  }

  showError(message: string) {
    console.error('Error:', message);
  }

  // Get unique categories
  getUniqueCategories(): string[] {
    const categories = this.events.map(event => event.category);
    return [...new Set(categories)];
  }

  // View rejection reason
  viewRejectionReason(event: Event) {
    if (event.rejectionReason) {
      this.showSuccess(`Lý do từ chối: ${event.rejectionReason}`);
    } else {
      this.showError('Không có lý do từ chối được ghi nhận.');
    }
  }
}
