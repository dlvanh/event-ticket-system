import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TicketOrderService } from '../../../services/ticket-order.service';
import { isPlatformBrowser } from '@angular/common';
import { StatusTranslatePipe } from '../../../../status-translate.pipe';

@Component({
  selector: 'app-cancel',
  imports: [CommonModule, StatusTranslatePipe],
  templateUrl: './cancel.component.html',
  styleUrl: './cancel.component.css'
})
export class CancelComponent implements OnInit {
  userId!: number;
  ticketOrders: any[] = [];

  constructor(private ticketOrderService: TicketOrderService,
    @Inject(PLATFORM_ID) private platformId: object) { }

  ngOnInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      const storedId = localStorage.getItem('userId');
      if (storedId) {
        this.userId = +storedId;
        this.loadCancelledTickets(this.userId);
      } else {
        console.error('No userId found in localStorage');
      }
    }
  }

  loadCancelledTickets(userId: number) {
    this.ticketOrderService.getTicketsByUserId(userId).subscribe({
      next: (res) => {
        const orders = res.data || [];
        this.ticketOrders = orders
          .filter((order: any) => order.status === 'cancelled')
          .sort((a: any, b: any) => new Date(b.orderDate).getTime() - new Date(a.orderDate).getTime());
      },
      error: (err) => {
        console.error('Error loading pending orders:', err);
      }
    });
  }
}
