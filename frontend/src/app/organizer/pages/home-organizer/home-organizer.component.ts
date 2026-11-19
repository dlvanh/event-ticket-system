import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { OngoingComponent } from '../../tabs/ongoing/ongoing.component';
import { UpcomingComponent } from '../../tabs/upcoming/upcoming.component';
import { PastComponent } from '../../tabs/past/past.component';
import { PendingComponent } from '../../tabs/pending/pending.component';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { ListEventsService } from '../../services/list-events.service';

@Component({
  selector: 'app-home-organizer',
  standalone: true,
  imports: [
    RouterModule,
    CommonModule,
    FormsModule,
    OngoingComponent, 
    UpcomingComponent,
    PastComponent,
    PendingComponent,
  ],
  templateUrl: './home-organizer.component.html',
  styleUrl: './home-organizer.component.css'
})
export class HomeOrganizerComponent implements OnInit {
  selectedTab: string = 'ongoing';
  readonly TABS = ['upcoming', 'ongoing', 'past', 'pending'];
  events: any[] = [];
  filteredEvents: any[] = [];
  tabEvents: { [key: string]: any[] } = {};
  eventId!: number;
  eventData: any;
  selectedEvent: any = null;
  searchTerm: string = '';

  constructor(
    private http: HttpClient,
    private route: ActivatedRoute,
    private listEventsService: ListEventsService
  ) {}

  ngOnInit(): void {
    const token = localStorage.getItem('token');
    if (!token) {
      console.error('Không tìm thấy token đăng nhập!');
      return;
    }

    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });

    // 1. Gọi API lấy danh sách sự kiện
    this.http.get<any>('http://localhost:8080/api/events/by-organizer', { headers })
      .subscribe({
        next: res => {
          this.events = res.data.listEvents;
          this.updateTabEvents();
          this.filteredEvents = [...this.tabEvents[this.selectedTab] || []];
        },
      });

    // 2. Gọi API lấy sự kiện theo ID nếu cần
    const idFromUrl = this.route.snapshot.paramMap.get('id');
    if (idFromUrl) {
      this.eventId = Number(idFromUrl);
      this.listEventsService.getEventById(this.eventId.toString()).subscribe({
        next: data => {
          this.eventData = data.data;
          this.selectedEvent = this.eventData;
        },
      });
    }
  }

  setTab(tab: string) {
    this.selectedTab = tab;
    this.filteredEvents = [...this.tabEvents[tab] || []];
    this.onSearchChange(); // Áp dụng tìm kiếm nếu có
  }

  onEventSelected(event: any) {
    this.selectedEvent = event;
  }

  // Tìm kiếm theo tên sự kiện
  onSearchChange(): void {
    const baseEvents = this.tabEvents[this.selectedTab] || [];
    if (!this.searchTerm.trim()) {
      this.filteredEvents = [...baseEvents];
    } else {
      const searchLower = this.searchTerm.toLowerCase().trim();
      this.filteredEvents = baseEvents.filter(event => {
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
  }

  // Cập nhật danh sách sự kiện cho từng tab
  updateTabEvents() {
    this.tabEvents = {
      // Đang diễn ra: status = 'upcoming', approvalStatus = 'approved'
      ongoing: this.events.filter(e => e.status === 'upcoming' && (e.approvalStatus === 'approved' || e.approval_status === 'approved')),
      // Sắp tới: status = 'upcoming', approvalStatus != 'approved'
      upcoming: this.events.filter(e => e.status === 'upcoming' && (e.approvalStatus !== 'approved' && e.approval_status !== 'approved')),
      // Đã qua: status = 'completed'
      past: this.events.filter(e => e.status === 'completed'),
      // Chờ duyệt: approvalStatus = 'pending'
      pending: this.events.filter(e => e.approvalStatus === 'pending' || e.approval_status === 'pending'),
    };
  }

  clearSearch(): void {
    this.searchTerm = '';
    this.filteredEvents = [...this.events];
  }
}
