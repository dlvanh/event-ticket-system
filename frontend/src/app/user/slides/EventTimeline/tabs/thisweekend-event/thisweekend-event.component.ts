import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EventsService } from '../../../../services/events.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-thisweekend-event',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './thisweekend-event.component.html',
  styleUrls: ['./thisweekend-event.component.css']
})
export class ThisweekendEventComponent implements OnInit {
  items: any[] = [];
  visibleItems: any[] = [];
  startIndex = 0;
  readonly ITEMS_PER_PAGE = 4;

  constructor(private eventsService: EventsService, private router: Router) {}

  ngOnInit(): void {
    this.loadWeekendEventsThisWeek();
  }

  
  loadWeekendEventsThisWeek() {
    const { startTimeISO, endTimeISO, startDate, endDate } = this.getWeekendTimeRange();

    this.eventsService.getRecommendedEvents(
      '', 
      undefined, 
      startTimeISO,
      endTimeISO,
      undefined, 
      '',
      1,
      50
    ).subscribe(res => {
      const allEvents = res?.data?.listEvents || [];
      
      const filteredEvents = allEvents.filter((event: any) => {
        const raw = event.startTime.replace(' ', 'T') + '+07:00';
        const eventTime = new Date(raw);
        
        const isInWeekend = eventTime.getTime() >= startDate.getTime() && eventTime.getTime() <= endDate.getTime();
        return isInWeekend;
      });

      this.items = filteredEvents.map((event: any) => ({
        id: event.eventId,
        backgroundUrl: event.backgroundUrl,
        startTime: event.startTime,
        eventName: event.eventName,
        price: event.minPrice,
        date: this.formatDate(event.startTime)
      }));

      this.startIndex = 0;
      this.updateVisibleItems();
    });
  }

  getWeekendTimeRange(): { startTimeISO: string, endTimeISO: string, startDate: Date, endDate: Date } {
    const now = new Date();
    const currentDayOfWeek = now.getDay();
    
    let daysToSaturday: number;
    if (currentDayOfWeek === 0) { 
      daysToSaturday = 6; 
    } else {
      daysToSaturday = 6 - currentDayOfWeek; 
    }
    
    const startDate = new Date(now);
    startDate.setDate(now.getDate() + daysToSaturday);
    startDate.setHours(0, 0, 0, 0);
    
    const endDate = new Date(startDate);
    endDate.setDate(startDate.getDate() + 1);
    endDate.setHours(23, 59, 59, 999);
    
    const startTimeISO = this.toISOWithTimezoneOffset(startDate, 7);
    const endTimeISO = this.toISOWithTimezoneOffset(endDate, 7);
    
    return { startTimeISO, endTimeISO, startDate, endDate };
  }

  toISOWithTimezoneOffset(date: Date, offsetHours: number): string {
    const offsetDate = new Date(date.getTime() - offsetHours * 60 * 60 * 1000);
    return offsetDate.toISOString().replace('Z', `+${offsetHours.toString().padStart(2, '0')}:00`);
  }

  formatDate(isoDate: string): string {
    const date = new Date(isoDate);
    const day = date.getDate().toString().padStart(2, '0');
    const month = (date.getMonth() + 1).toString().padStart(2, '0');
    const year = date.getFullYear();
    return `${day} tháng ${month}, ${year}`;
  }

  updateVisibleItems() {
    this.visibleItems = this.items.slice(this.startIndex, this.startIndex + this.ITEMS_PER_PAGE);
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
    return this.startIndex + this.ITEMS_PER_PAGE < this.items.length;
  }

  goToEventDetail(eventId: number) {
    // Lưu đường dẫn vào sessionStorage
    sessionStorage.setItem('redirectAfterLogin', `/detail-ticket/${eventId}`);
    this.router.navigate(['/detail-ticket', eventId]);
  }
}
