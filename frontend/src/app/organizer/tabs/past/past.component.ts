import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ListEventsService} from '../../services/list-events.service';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-past',
  imports: [CommonModule, RouterModule],
  templateUrl: './past.component.html',
  styleUrl: './past.component.css'
})
export class PastComponent {
  @Input() events: any[] = [];

  selectedEvent: any = null;
  isLoadingDetail: boolean = false;

  constructor(private listEventsService: ListEventsService) {}

  get pastEvents() {
    const now = new Date();
    return this.events?.filter(event => new Date(event.endTime) < now) || [];
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
        console.error('Lỗi khi lấy chi tiết sự kiện:', err);
        this.isLoadingDetail = false;
      }
    });
  }

  closeDetail() {
    this.selectedEvent = null;
  }
}
