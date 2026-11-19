import { Component } from '@angular/core';
import { Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { StatusTranslatePipe } from '../../../status-translate.pipe';
import { ListEventsService} from '../../services/list-events.service';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-pending',
  standalone: true,
  imports: [CommonModule, StatusTranslatePipe, RouterModule, FormsModule],
  templateUrl: './pending.component.html',
  styleUrl: './pending.component.css'
})
export class PendingComponent {
  @Input() events: any[] = [];

  selectedEvent: any = null;
  isLoadingDetail:boolean = false;
  isEditing: boolean = false;
  editableEvent: any = null;
  logoFile: File | null = null;
  backgroundFile: File | null = null;

  constructor(private listEventsService: ListEventsService) {}

  get pendingEvents() {
  return this.events?.filter(event => event.approvalStatus === 'pending') || [];
  }

  openDetail(event: any) {
    const eventId = event.eventId;
    this.isLoadingDetail = true;

    this.listEventsService.getEventById(eventId.toString()).subscribe({
      next: (res) => {
        this.selectedEvent = res.data;
        this.editableEvent = JSON.parse(JSON.stringify(res.data));
        this.isLoadingDetail = false;
      },
      error: (err) => {
        this.isLoadingDetail = false;
      }
    });
  }

  closeDetail() {
    this.selectedEvent = null;
    this.isEditing = false;
    this.editableEvent = null;
    this.logoFile = null;
    this.backgroundFile = null;
  }

  toggleEdit() {
    this.isEditing = !this.isEditing;
    if (!this.isEditing) {
      this.editableEvent = JSON.parse(JSON.stringify(this.selectedEvent));
      this.logoFile = null;
      this.backgroundFile = null;
    }
  }

  onFileSelected(event: any, fileType: 'logo' | 'background') {
    const file = event.target.files[0];
    if (file) {
      if (fileType === 'logo') {
        this.logoFile = file;
      } else {
        this.backgroundFile = file;
      }
    }
  }

  saveChanges() {
    if (!this.editableEvent) return;
    
    const { eventName, startTime, endTime, address, description, category } = this.editableEvent;
    const updateDto = { eventName, startTime, endTime, address, description, category };

    this.listEventsService.updateEvent(this.editableEvent.eventId, updateDto, this.logoFile || undefined, this.backgroundFile || undefined)
      .subscribe({
        next: (res) => {
          this.isEditing = false;
          this.selectedEvent = { ...this.editableEvent };
          const index = this.events.findIndex(e => e.eventId === this.selectedEvent.eventId);
          if (index !== -1) {
              this.events[index] = { ...this.events[index], ...this.editableEvent };
          }
          this.closeDetail();
        },
        error: (err) => {
        }
      });
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
