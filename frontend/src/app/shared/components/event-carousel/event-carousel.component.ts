import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { EventsService } from '../../../user/services/events.service';
import { EventCardItem } from '../../../core/models';

@Component({
  selector: 'app-event-carousel',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './event-carousel.component.html',
  styleUrls: ['./event-carousel.component.css']
})
export class EventCarouselComponent implements OnInit {
  @Input({ required: true }) title!: string;
  @Input({ required: true }) category!: string;
  @Input() itemsPerPage = 4;

  events: EventCardItem[] = [];
  visibleItems: EventCardItem[] = [];
  startIndex = 0;

  constructor(private eventService: EventsService, private router: Router) {}

  ngOnInit(): void {
    this.loadEvents();
  }

  private loadEvents(): void {
    this.eventService.getRecommendedEvents(this.category).subscribe({
      next: res => {
        const list = res?.data?.listEvents || [];
        this.events = list.map((event: any) => ({
          id: event.eventId,
          eventName: event.eventName,
          description: event.description,
          startTime: event.startTime,
          endTime: event.endTime,
          category: event.category,
          status: event.status,
          approvalStatus: event.approval_status ?? event.approvalStatus,
          backgroundUrl: event.backgroundUrl,
          address: event.address || event.addressName,
          addressDetail: event.addressDetail,
          price: event.minPrice
        }));
        this.startIndex = 0;
        this.updateVisibleItems();
      },
      error: err => {
        console.error(`Failed to load events for category ${this.category}:`, err);
      }
    });
  }

  private updateVisibleItems(): void {
    this.visibleItems = this.events.slice(this.startIndex, this.startIndex + this.itemsPerPage);
  }

  onSeeMore(): void {
    this.router.navigate(['/search-events'], {
      queryParams: { category: this.category }
    });
  }

  goToEventDetail(eventId: number): void {
    sessionStorage.setItem('redirectAfterLogin', `/detail-ticket/${eventId}`);
    this.router.navigate(['/detail-ticket', eventId]);
  }
}
