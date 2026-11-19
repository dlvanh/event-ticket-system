import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

@Component({
  selector: 'app-export-file',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './export-file.component.html',
  styleUrl: './export-file.component.css'
})
export class ExportFileComponent implements OnInit {
  events: any[] = [];
  filteredEvents: any[] = [];
  selectedEvent: any = null;
  isLoadingList = false;
  startDate: string = '';
  endDate: string = '';
  showFilter = false;
  searchTerm: string = '';

  // Phân trang
  currentPage: number = 1;
  pageSize: number = 10;
  totalPages: number = 1;
  pagedEvents: any[] = [];

  constructor(private http: HttpClient, private router: Router) {}

  ngOnInit(): void {
    const token = localStorage.getItem('token');
    if (!token) {
      this.router.navigate(['/login']);
      return;
    }
    this.fetchEvents(token);
  }

  fetchEvents(token: string): void {
    this.isLoadingList = true;
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });

    this.http.get<any>('http://localhost:8080/api/events/by-organizer', { headers })
      .subscribe({
        next: (res) => {
          this.events = res.data?.listEvents || [];
          this.filterEvents();
          this.isLoadingList = false;
        },
        error: (err) => {
          this.isLoadingList = false;
        }
      });
  }

  filterEvents() {
    // Nếu không chọn ngày nào thì hiển thị toàn bộ sự kiện
    if (!this.startDate && !this.endDate) {
      this.filteredEvents = [...this.events];
    } else {
      const start = this.startDate ? new Date(this.startDate).getTime() : -Infinity;
      const end = this.endDate ? new Date(this.endDate).getTime() : Infinity;
      this.filteredEvents = this.events.filter(event => {
        const eventTime = new Date(event.startTime).getTime();
        return eventTime >= start && eventTime <= end;
      });
    }
    this.showFilter = false;
    this.currentPage = 1;
    this.updatePagedEvents();
  }

  onSearchChange(): void {
    if (!this.searchTerm.trim()) {
      this.filteredEvents = [...this.events];
    } else {
      const searchLower = this.searchTerm.toLowerCase().trim();
      this.filteredEvents = this.events.filter(event => {
        // Tìm kiếm theo tên sự kiện
        const nameMatch = event.eventName && event.eventName.toLowerCase().includes(searchLower);
        // Tìm kiếm theo địa điểm
        const venueMatch = event.addressName && event.addressName.toLowerCase().includes(searchLower);
        // Tìm kiếm theo thể loại
        const categoryMatch = event.category && event.category.toLowerCase().includes(searchLower);
        // Tìm kiếm theo mô tả
        const descriptionMatch = event.description && event.description.toLowerCase().includes(searchLower);
        return nameMatch || venueMatch || categoryMatch || descriptionMatch;
      });
    }
    this.currentPage = 1;
    this.updatePagedEvents();
  }

  // Xóa tìm kiếm
  clearSearch(): void {
    this.searchTerm = '';
    this.filteredEvents = [...this.events];
    this.currentPage = 1;
    this.updatePagedEvents();
  }

  // Cập nhật danh sách sự kiện theo trang
  updatePagedEvents(): void {
    this.totalPages = Math.ceil(this.filteredEvents.length / this.pageSize) || 1;
    if (this.currentPage > this.totalPages) this.currentPage = this.totalPages;
    const startIdx = (this.currentPage - 1) * this.pageSize;
    const endIdx = startIdx + this.pageSize;
    this.pagedEvents = this.filteredEvents.slice(startIdx, endIdx);
  }

  goToPage(page: number): void {
    if (page < 1 || page > this.totalPages) return;
    this.currentPage = page;
    this.updatePagedEvents();
  }

  prevPage(): void {
    if (this.currentPage > 1) {
      this.currentPage--;
      this.updatePagedEvents();
    }
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
      this.updatePagedEvents();
    }
  }

  openDetail(event: any): void {
    this.selectedEvent = event;
  }

  closeDetail(): void {
    this.selectedEvent = null;
  }

  // Khi filteredEvents thay đổi, cập nhật pagedEvents
  ngDoCheck(): void {
    this.updatePagedEvents();
  }
}
