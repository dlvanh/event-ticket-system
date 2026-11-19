import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { TicketOrderService } from '../../../services/ticket-order.service';
import { StatusTranslatePipe } from '../../../../status-translate.pipe';

@Component({
  selector: 'app-success',
  standalone: true,
  imports: [CommonModule, StatusTranslatePipe],
  templateUrl: './success.component.html',
  styleUrl: './success.component.css'
})
export class SuccessComponent implements OnInit {
  userId!: number;
  ticketOrders: any[] = [];
  allTickets: any[] = [];
  selectedTab: string = 'all';

  constructor(
    private ticketOrderService: TicketOrderService,
    @Inject(PLATFORM_ID) private platformId: object
  ) {}

  ngOnInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      const storedId = localStorage.getItem('userId');
      if (storedId) {
        this.userId = +storedId;
        this.loadSuccessTickets(this.userId);
      } else {
        console.error('No userId found in localStorage');
      }
    }
  }

  setTab(tab: string) {
    this.selectedTab = tab;
    this.applyFilter();
  }

  isTabActive(tab: string): boolean {
    return this.selectedTab === tab;
  }

  loadSuccessTickets(userId: number) {
    this.ticketOrderService.getTicketsByUserId(userId).subscribe({
      next: (res) => {
        const orders = res.data || [];
        const paidOrders = orders
          .filter((order: any) => order.status === 'paid')
          .sort((a: any, b: any) =>
            new Date(b.orderDate).getTime() - new Date(a.orderDate).getTime()
          );

        this.allTickets = paidOrders;
        this.applyFilter();
      },
      error: (err) => {
        console.error('Error loading paid orders:', err);
      }
    });
  }

  private applyFilter() {
    const now = new Date().getTime();

    switch (this.selectedTab) {
      case 'coming':
        this.ticketOrders = this.allTickets.filter(order =>
          order.tickets?.some((ticket: any) =>
            new Date(ticket.eventStartTime).getTime() > now
          )
        );
        break;

      case 'ended':
        this.ticketOrders = this.allTickets.filter(order =>
          order.tickets?.every((ticket: any) =>
            new Date(ticket.eventEndTime).getTime() < now
          )
        );
        break;

      case 'all':
      default:
        this.ticketOrders = [...this.allTickets];
        break;
    }
  }
}
