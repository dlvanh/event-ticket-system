import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { OngoingComponent } from '../../tabs/ongoing/ongoing.component';
import { UpcomingComponent } from '../../tabs/upcoming/upcoming.component';
import { PastComponent } from '../../tabs/past/past.component';
import { PendingComponent } from '../../tabs/pending/pending.component';
import { ListEventsService } from '../../services/list-events.service';
import { AuthService } from '../../../auth/services/auth.service';

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
  selectedTab = 'ongoing';
  readonly TABS = ['upcoming', 'ongoing', 'past', 'pending'];
  events: any[] = [];
  filteredEvents: any[] = [];
  tabEvents: Record<string, any[]> = {};
  eventId!: number;
  eventData: any;
  selectedEvent: any = null;
  searchTerm = '';

  constructor(
    private route: ActivatedRoute,
    private listEventsService: ListEventsService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    if (!this.authService.getToken()) {
      return;
    }

    this.listEventsService.getEventsByOrganizer().subscribe({
      next: res => {
        this.events = res?.data?.listEvents || [];
        this.updateTabEvents();
        this.filteredEvents = [...(this.tabEvents[this.selectedTab] || [])];
      }
    });

    const idFromUrl = this.route.snapshot.paramMap.get('id');
    if (idFromUrl) {
      this.eventId = Number(idFromUrl);
      this.listEventsService.getEventById(this.eventId).subscribe({
        next: data => {
          this.eventData = data?.data;
          this.selectedEvent = this.eventData;
        }
      });
    }
  }

  setTab(tab: string): void {
    this.selectedTab = tab;
    this.filteredEvents = [...(this.tabEvents[tab] || [])];
    this.onSearchChange();
  }

  onEventSelected(event: any): void {
    this.selectedEvent = event;
  }

  onSearchChange(): void {
    const baseEvents = this.tabEvents[this.selectedTab] || [];
    if (!this.searchTerm.trim()) {
      this.filteredEvents = [...baseEvents];
    } else {
      const searchLower = this.searchTerm.toLowerCase().trim();
      this.filteredEvents = baseEvents.filter(event => {
        const nameMatch = event.eventName && event.eventName.toLowerCase().includes(searchLower);
        const venueMatch = event.addressName && event.addressName.toLowerCase().includes(searchLower);
        const categoryMatch = event.category && event.category.toLowerCase().includes(searchLower);
        const descriptionMatch = event.description && event.description.toLowerCase().includes(searchLower);
        return nameMatch || venueMatch || categoryMatch || descriptionMatch;
      });
    }
  }

  updateTabEvents(): void {
    this.tabEvents = {
      ongoing: this.events.filter(e => e.status === 'upcoming' && (e.approvalStatus === 'approved' || e.approval_status === 'approved')),
      upcoming: this.events.filter(e => e.status === 'upcoming' && (e.approvalStatus !== 'approved' && e.approval_status !== 'approved')),
      past: this.events.filter(e => e.status === 'completed'),
      pending: this.events.filter(e => e.approvalStatus === 'pending' || e.approval_status === 'pending'),
    };
  }

  clearSearch(): void {
    this.searchTerm = '';
    this.filteredEvents = [...(this.tabEvents[this.selectedTab] || [])];
  }
}
