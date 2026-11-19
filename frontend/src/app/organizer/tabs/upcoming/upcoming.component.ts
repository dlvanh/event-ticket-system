import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { StatusTranslatePipe } from '../../../status-translate.pipe';
import { ListEventsService } from '../../services/list-events.service';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-upcoming',
  standalone: true,
  imports: [CommonModule, StatusTranslatePipe, RouterModule],
  templateUrl: './upcoming.component.html',
  styleUrl: './upcoming.component.css'
})
export class UpcomingComponent {
  @Input() events: any[] = [];

  selectedEvent: any = null;
  isLoadingDetail: boolean = false;

  // Phân trang
  currentPage: number = 1;
  pageSize: number = 10;
  totalPages: number = 1;
  pagedEvents: any[] = [];

  constructor(private listEventsService: ListEventsService) {}

  get upcomingEvents() {
    // Hiển thị các sự kiện có trạng thái 'pending'
    return this.events?.filter(event => event.approvalStatus === 'pending') ?? [];
  }

  ngOnChanges() {
    this.currentPage = 1;
    this.updatePagedEvents();
  }

  ngDoCheck() {
    this.updatePagedEvents();
  }

  updatePagedEvents() {
    const events = this.upcomingEvents;
    this.totalPages = Math.ceil(events.length / this.pageSize) || 1;
    if (this.currentPage > this.totalPages) this.currentPage = this.totalPages;
    const startIdx = (this.currentPage - 1) * this.pageSize;
    const endIdx = startIdx + this.pageSize;
    this.pagedEvents = events.slice(startIdx, endIdx);
  }

  goToPage(page: number) {
    if (page < 1 || page > this.totalPages) return;
    this.currentPage = page;
    this.updatePagedEvents();
  }

  prevPage() {
    if (this.currentPage > 1) {
      this.currentPage--;
      this.updatePagedEvents();
    }
  }

  nextPage() {
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
      this.updatePagedEvents();
    }
  }

  openDetail(event: any) {
    const eventId = event.eventId;
    this.isLoadingDetail = true;

    this.listEventsService.getEventById(eventId).subscribe({
      next: (res) => {
        this.selectedEvent = res.data;
        this.isLoadingDetail = false;
      },
      error: (err) => {
        this.isLoadingDetail = false;
      }
    });
  }

  closeDetail() {
    this.selectedEvent = null;
  }
  
  asNumber(value: unknown): number {
    return Number(value);
  }

  trackByEventId(index: number, event: any): any {
    return event?.eventId || index;
  }

  get ticketPriceList(): { type: string; price: number }[] {
    if (!this.selectedEvent?.ticketPrices) return [];
    return Object.entries(this.selectedEvent.ticketPrices).map(([type, price]) => ({
      type,
      price: Number(price)
    }));
  }
}
