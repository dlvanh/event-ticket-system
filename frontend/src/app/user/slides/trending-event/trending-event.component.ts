import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EventsService } from '../../services/events.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-trending-event',
  imports: [CommonModule],
  templateUrl: './trending-event.component.html',
  styleUrl: './trending-event.component.css'
})
export class TrendingEventComponent implements OnInit {
  events: any[] = [];
  visibleItems: any[] = [];
  startIndex: number = 0;
  readonly ITEMS_PER_PAGE = 3;

  constructor(private eventService: EventsService,private router: Router) {}

  ngOnInit(): void {
    this.loadTrendingEvents();
  }

  loadTrendingEvents(): void {
    this.eventService.getRecommendedEvents('trending').subscribe(res => {
      const events = res?.data?.listEvents || [];

      this.events = events.map((event: any) => ({
        id: event.eventId,
        backgroundUrl: event.backgroundUrl,
      }));

      this.startIndex = 0;
      this.updateVisibleItems();
    });
  }

  updateVisibleItems() {
    this.visibleItems = this.events.slice(this.startIndex, this.startIndex + this.ITEMS_PER_PAGE);
  }

  scrollLeft() {
    if (this.canScrollLeft) {
      this.startIndex -= this.ITEMS_PER_PAGE;
      this.updateVisibleItems();
    }
  }

  scrollRight() {
    if (this.canScrollRight) {
      this.startIndex += this.ITEMS_PER_PAGE;
      this.updateVisibleItems();
    }
  }

  get canScrollLeft(): boolean {
    return this.startIndex > 0;
  }

  get canScrollRight(): boolean {
    return this.startIndex + this.ITEMS_PER_PAGE < this.events.length;
  }

  goToEventDetail(eventId: number) {
    // Lưu đường dẫn vào sessionStorage
    sessionStorage.setItem('redirectAfterLogin', `/detail-ticket/${eventId}`);
    this.router.navigate(['/detail-ticket', eventId]);
  }
}
