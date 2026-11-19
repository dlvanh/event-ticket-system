import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { TrendingEventComponent } from '../../slides/trending-event/trending-event.component';
import { ForyouEventComponent } from '../../slides/foryou-event/foryou-event.component';
import { TimelineEventComponent } from '../../slides/EventTimeline/timeline-event/timeline-event.component';
import { InterestPlaceComponent } from '../../slides/interest-place/interest-place.component';
import { SpecialEventComponent } from '../../slides/special-event/special-event.component';
import { EventCarouselComponent } from '../../../shared/components/event-carousel/event-carousel.component';
import { ChatbotComponent } from '../../pop-up/chatbot/chatbot.component';
import { EventsService } from '../../services/events.service';

@Component({
  selector: 'app-home-user',
  standalone: true,
  imports: [
    CommonModule,
    TrendingEventComponent,
    ForyouEventComponent,
    TimelineEventComponent,
    InterestPlaceComponent,
    SpecialEventComponent,
    EventCarouselComponent,
    ChatbotComponent
  ],
  templateUrl: './home-user.component.html',
  styleUrl: './home-user.component.css'
})
export class HomeUserComponent implements OnInit {
  events: { id: number; backgroundUrl: string }[] = [];
  currentIndex = 0;
  transitionStyle = 'transform 0.5s ease-in-out';

  readonly categoryCarousels = [
    { title: 'Nhạc sống', category: 'Music' },
    { title: 'Sân khấu & Nghệ thuật', category: 'Theatre-Arts' },
    { title: 'Thể thao', category: 'Sport' },
    { title: 'Workshop', category: 'Workshop' },
    { title: 'Khác', category: 'Other' }
  ];

  constructor(private eventService: EventsService, private router: Router) {}

  ngOnInit(): void {
    this.loadHotTrendingEvents();
  }

  loadHotTrendingEvents(): void {
    this.eventService.getRecommendedEvents(
      undefined,
      undefined,
      undefined,
      undefined,
      undefined,
      'totalTicketSold:DESC',
      1,
      100
    ).subscribe(res => {
      const allEvents = res?.data?.listEvents || [];
      this.events = allEvents.slice(0, 4).map((event: any) => ({
        id: event.eventId,
        backgroundUrl: event.backgroundUrl
      }));
    });
  }

  goToSearchEvents(category: string): void {
    const categoryMap: Record<string, string> = {
      'Nhạc sống': 'Music',
      'Sân khấu & Nghệ thuật': 'Theatre-Arts',
      'Thể Thao': 'Sport',
      'Workshop': 'Workshop',
      'Khác': 'Other'
    };

    const categoryParam = categoryMap[category];
    this.router.navigate(['/search-events'], {
      queryParams: { category: categoryParam }
    });
  }

  get visibleImages() {
    const total = this.events.length;
    if (total === 0) return [];

    const first = this.currentIndex % total;
    const second = (this.currentIndex + 1) % total;

    return [
      this.events[first],
      this.events.length > 1 ? this.events[second] : null
    ].filter((item): item is { id: number; backgroundUrl: string } => item !== null);
  }

  get dotIndicators(): number[] {
    return Array(this.events.length).fill(0);
  }

  nextSlide(): void {
    if (this.events.length === 0) return;
    this.currentIndex = (this.currentIndex + 1) % this.events.length;
  }

  prevSlide(): void {
    if (this.events.length === 0) return;
    this.currentIndex = (this.currentIndex - 1 + this.events.length) % this.events.length;
  }

  goToEventDetail(eventId: number): void {
    sessionStorage.setItem('redirectAfterLogin', `/detail-ticket/${eventId}`);
    this.router.navigate([`/detail-ticket/${eventId}`]);
  }
}
