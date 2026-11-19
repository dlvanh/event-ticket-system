import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EventsService } from '../../services/events.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-others-event',
  imports: [CommonModule],
  templateUrl: './others-event.component.html',
  styleUrl: './others-event.component.css'
})
export class OthersEventComponent {
  events: any[] = [];
  visibleItems: any[] = [];
  startIndex: number = 0;
  readonly ITEMS_PER_PAGE = 4;

  constructor(private eventService: EventsService,private router: Router) {}

  ngOnInit(): void {
    this.loadOthersEvents();
  }

  loadOthersEvents() {
    this.eventService.getRecommendedEvents('Other').subscribe(res => {
      this.events = res?.data?.listEvents || [];
      
      this.events = this.events.map((event: any) => ({
        id: event.eventId,
        eventName: event.eventName,
        description: event.description,
        startTime: event.startTime,
        endTime: event.endTime,
        category: event.category,
        status: event.status,
        approval_status: event.approval_status,
        backgroundUrl: event.backgroundUrl,
        address: event.address || event.addressName,
        addressDetail: event.addressDetail,
        price: event.minPrice
      }));
      
      this.startIndex = 0;
      this.updateVisibleItems();
    });
  }

  updateVisibleItems() {
    this.visibleItems = this.events.slice(this.startIndex, this.startIndex + this.ITEMS_PER_PAGE);
  }

  onSeeMore() {
    this.router.navigate(['/search-events'], {
      queryParams: { category: 'Other' }
    });
  }

  goToEventDetail(eventId: number) {
    // Lưu đường dẫn vào sessionStorage
    sessionStorage.setItem('redirectAfterLogin', `/detail-ticket/${eventId}`);
    this.router.navigate(['/detail-ticket', eventId]);
  }
}
