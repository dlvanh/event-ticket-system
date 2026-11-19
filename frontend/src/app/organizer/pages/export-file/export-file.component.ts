import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ListEventsService } from '../../services/list-events.service';
import { AuthService } from '../../../auth/services/auth.service';

@Component({
  selector: 'app-export-file',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './export-file.component.html',
  styleUrl: './export-file.component.css'
})
export class ExportFileComponent implements OnInit {
  events: any[] = [];
  filteredEvents: any[] = [];
  selectedEvent: any = null;
  isLoadingList = false;
  startDate = '';
  endDate = '';
  showFilter = false;
  searchTerm = '';

  currentPage = 1;
  pageSize = 10;
  totalPages = 1;
  pagedEvents: any[] = [];

  constructor(
    private listEventsService: ListEventsService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    if (!this.authService.getToken()) {
      this.router.navigate(['/login']);
      return;
    }
    this.fetchEvents();
  }

  fetchEvents(): void {
    this.isLoadingList = true;
    this.listEventsService.getEventsByOrganizer().subscribe({
      next: res => {
        this.events = res?.data?.listEvents || [];
        this.filterEvents();
        this.isLoadingList = false;
      },
      error: () => {
        this.isLoadingList = false;
      }
    });
  }

  filterEvents(): void {
    if (!this.startDate && !this.endDate) {
      this.filteredEvents = [...this.events];
    } else {
      const start = this.startDate ? new Date(this.startDate).getTime() : -Infinity;
      const end = this.endDate ? new Date(this.endDate).getTime() : Infinity;
      this.filteredEvents = this.events.filter(event => {
        const eventTime = new Date(event.startTime).getTime();
        return eventTime >= start && eventTime <= end;
      });
    }
    this.showFilter = false;
    this.currentPage = 1;
    this.updatePagedEvents();
  }

  onSearchChange(): void {
    if (!this.searchTerm.trim()) {
      this.filteredEvents = [...this.events];
    } else {
      const searchLower = this.searchTerm.toLowerCase().trim();
      this.filteredEvents = this.events.filter(event => {
        const nameMatch = event.eventName && event.eventName.toLowerCase().includes(searchLower);
        const venueMatch = event.addressName && event.addressName.toLowerCase().includes(searchLower);
        const categoryMatch = event.category && event.category.toLowerCase().includes(searchLower);
        const descriptionMatch = event.description && event.description.toLowerCase().includes(searchLower);
        return nameMatch || venueMatch || categoryMatch || descriptionMatch;
      });
    }
    this.currentPage = 1;
    this.updatePagedEvents();
  }

  clearSearch(): void {
    this.searchTerm = '';
    this.filteredEvents = [...this.events];
    this.currentPage = 1;
    this.updatePagedEvents();
  }

  updatePagedEvents(): void {
    this.totalPages = Math.ceil(this.filteredEvents.length / this.pageSize) || 1;
    if (this.currentPage > this.totalPages) this.currentPage = this.totalPages;
    const startIdx = (this.currentPage - 1) * this.pageSize;
    this.pagedEvents = this.filteredEvents.slice(startIdx, startIdx + this.pageSize);
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
    this.selectedEvent = event;
  }

  closeDetail(): void {
    this.selectedEvent = null;
  }
}
