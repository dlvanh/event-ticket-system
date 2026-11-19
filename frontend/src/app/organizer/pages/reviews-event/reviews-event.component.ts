import { Component, OnInit, ViewChild } from '@angular/core';
import { ReviewsService } from '../../services/reviews.service';
import { ListEventsService } from '../../services/list-events.service';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ToastNotificationComponent } from '../../../user/pop-up/toast-notification/toast-notification.component';

@Component({
  selector: 'app-reviews-event',
  templateUrl: './reviews-event.component.html',
  styleUrl: './reviews-event.component.css',
  standalone: true,
  imports: [CommonModule, DatePipe, FormsModule, ToastNotificationComponent]
})
export class ReviewsEventComponent implements OnInit {
  @ViewChild('notification') notification!: ToastNotificationComponent;

  shownotification: boolean = false;

  events: any[] = [];
  filteredEvents: any[] = [];
  selectedEvent: any = null;
  reviews: any[] = [];
  isLoadingEvents = false;
  isLoadingReviews = false;
  searchTerm: string = '';
  selectedStar: number | null = null;
  showFilter = false;

  // Phân trang
  currentPage: number = 1;
  toggleFilter(): void {
    this.showFilter = !this.showFilter;
  }
  pageSize: number = 10;
  totalPages: number = 1;
  pagedEvents: any[] = [];

  constructor(
    private listEventsService: ListEventsService,
    private reviewsService: ReviewsService
  ) {}

  ngOnInit(): void {
    this.fetchEvents();
    this.filteredEvents = [...this.events];
  }

  fetchEvents() {
    this.isLoadingEvents = true;
    this.listEventsService.getEventsByOrganizer().subscribe({
      next: res => {
        this.events = res?.data?.listEvents || [];
        this.isLoadingEvents = false;

        this.filteredEvents = [...this.events];
        this.updatePagedEvents();

        this.events.forEach(event => {
          const eventId = event.id || event.eventId;
          if (eventId && event.status === 'completed') {
            this.reviewsService.getReviewsByEventId(eventId).subscribe({
              next: reviewRes => {
                event.reviewDetails = reviewRes?.reviewDetails || [];
              },
              error: err => {
                event.reviewDetails = [];
              }
            });
          } else {
            event.reviewDetails = [];
          }
        });
      },
      error: err => {
        console.error('Lỗi khi lấy danh sách sự kiện:', err);
        this.isLoadingEvents = false;
      }
    });
  }
  updatePagedEvents(): void {
    const startIndex = (this.currentPage - 1) * this.pageSize;
    const endIndex = startIndex + this.pageSize;
    this.pagedEvents = this.filteredEvents.slice(startIndex, endIndex);
    this.totalPages = Math.ceil(this.filteredEvents.length / this.pageSize);
  }

  selectEvent(event: any) {
    if (event.status !== 'completed') {
      this.notification.showNotification('Không thể xem đánh giá khi sự kiện chưa kết thúc!', 5000, 'warning');
      return;
    }
    this.selectedEvent = event;
    this.fetchReviews(event.id || event.eventId);
  }

  fetchReviews(eventId: number | string) {
    this.isLoadingReviews = true;
    this.reviewsService.getReviewsByEventId(eventId).subscribe({
      next: res => {
        this.reviews = res?.reviewDetails || [];
        this.isLoadingReviews = false;
      },
      error: err => {
        this.isLoadingReviews = false;
      }
    });
  }

  getAverageRating(reviewsArr?: any[]): number | null {
    const arr = Array.isArray(reviewsArr) ? reviewsArr : this.reviews;
    if (!Array.isArray(arr) || arr.length === 0) return null;
    const sum = arr.reduce((acc: number, r: any) => acc + (typeof r.rating === 'number' ? r.rating : 0), 0);
    return Math.round((sum / arr.length) * 100) / 100;
  }

  backToList() {
    this.selectedEvent = null;
    this.reviews = [];
  }

  trackByEventId(index: number, event: any): number {
    return event.id || event.eventId;
  }

  onSearchChange(): void {
    let filtered = [...this.events];
    if (this.searchTerm.trim()) {
      const searchLower = this.searchTerm.toLowerCase().trim();
      filtered = filtered.filter(event => {
        const nameMatch = event.eventName && event.eventName.toLowerCase().includes(searchLower);
        const venueMatch = event.addressName && event.addressName.toLowerCase().includes(searchLower);
        const categoryMatch = event.category && event.category.toLowerCase().includes(searchLower);
        const descriptionMatch = event.description && event.description.toLowerCase().includes(searchLower);
        return nameMatch || venueMatch || categoryMatch || descriptionMatch;
      });
    }
    if (this.selectedStar) {
      filtered = filtered.filter(event => {
        if (!event.reviewDetails || event.reviewDetails.length === 0) return false;
        const avg = this.getAverageRating(event.reviewDetails);
        return avg !== null && Math.floor(avg) === this.selectedStar;
      });
    }
    this.filteredEvents = filtered;
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
  // Xử lý thay đổi bộ lọc số sao
  onStarFilterChange(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.selectedStar = value ? Number(value) : null;
    this.onSearchChange();
  }
  onNotificationClose() {}
}
