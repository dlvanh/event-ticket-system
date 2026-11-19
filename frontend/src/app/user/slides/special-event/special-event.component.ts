import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EventsService } from '../../services/events.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-special-event',
  imports: [CommonModule],
  templateUrl: './special-event.component.html',
  styleUrl: './special-event.component.css'
})
export class SpecialEventComponent {
  events: any[] = [];
  visibleItems: any[] = [];
  startIndex: number = 0;
  readonly ITEMS_PER_PAGE = 4;

  constructor(private eventService: EventsService,private router: Router) {}

  ngOnInit(): void {
    this.loadSpecialEvents();
  }

  loadSpecialEvents() {
    this.eventService.getRecommendedEvents('special-event').subscribe(res => {
      this.events = res?.data?.listEvents || [];
      

      // Map lại nếu cần, đảm bảo có đủ các trường
      this.events = this.events.map((event: any) => ({
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
