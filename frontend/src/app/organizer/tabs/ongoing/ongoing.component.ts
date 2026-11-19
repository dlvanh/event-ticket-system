import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { StatusTranslatePipe } from '../../../status-translate.pipe';
import { ListEventsService } from '../../services/list-events.service';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-ongoing',
  standalone: true,
  imports: [CommonModule, StatusTranslatePipe, RouterModule],
  templateUrl: './ongoing.component.html',
  styleUrls: ['./ongoing.component.css']
})
export class OngoingComponent implements OnChanges {
  @Input() events: any[] = [];

  selectedEvent: any = null;
  isLoadingDetail: boolean = false;
  currentPage: number = 1;
  pageSize: number = 10;
  totalPages: number = 1;
  pagedEvents: any[] = [];

  constructor(private listEventsService: ListEventsService) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['events']) {
      this.currentPage = 1;
      this.updatePagedEvents();
    }
  }

  updatePagedEvents(): void {
    const events = this.events || [];
    this.totalPages = Math.max(1, Math.ceil(events.length / this.pageSize));
    if (this.currentPage > this.totalPages) {
      this.currentPage = this.totalPages;
    }
    const startIdx = (this.currentPage - 1) * this.pageSize;
    const endIdx = startIdx + this.pageSize;
    this.pagedEvents = events.slice(startIdx, endIdx);
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
    if (!event?.eventId) return;

    this.isLoadingDetail = true;
    this.listEventsService.getEventById(event.eventId).subscribe({
      next: (res) => {
        this.selectedEvent = res.data;
        this.isLoadingDetail = false;
      },
      error: (err) => {
        this.isLoadingDetail = false;
      }
    });
  }

  closeDetail(): void {
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
