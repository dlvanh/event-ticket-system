import { Component, OnInit, HostListener,ViewChild  } from '@angular/core';
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { EventsService } from '../../services/events.service';
import { ToastNotificationComponent } from '../../pop-up/toast-notification/toast-notification.component';
import { ActivatedRoute } from '@angular/router';
import { TicketOrderService } from '../../services/ticket-order.service';

@Component({
  selector: 'app-select-ticket',
  imports: [CommonModule, ToastNotificationComponent],
  templateUrl: './select-ticket.component.html',
  styleUrls: ['./select-ticket.component.css']
})
export class SelectTicketComponent {
  eventId!: number;
  eventData: any = {};
  ticketList: any[] = [];
  events: any[] = [];

  @ViewChild('notification') notification!: ToastNotificationComponent;

  constructor(private router: Router,
    private eventsService: EventsService, 
    private route: ActivatedRoute,
    private ticketOrderService: TicketOrderService) {}
  
  ngOnInit(): void {
    this.route.params.subscribe(params => {
      this.eventId = +params['id'];
      this.loadEventDetail(this.eventId);

      // Check để tránh lỗi trong SSR
      if (typeof window !== 'undefined') {
        window.scrollTo({ top: 0, behavior: 'smooth' });
      }
    });
  }
  
  loadEventDetail(id: number): void {
    this.eventsService.getEventById(id).subscribe({
      next: (res) => {
        const event = res?.data;

        // Lấy giá đầu tiên từ ticketPrices
        const ticketTypesMap = event.ticketTypes || {}; 
        const ticketPricesObj = event.ticketPrices || {};
        const price = Object.values(ticketPricesObj)[0] || 0;

        // Chuyển ticketPrices object thành mảng để hiển thị danh sách
        const ticketPrices = Object.entries(ticketPricesObj).map(([type, price]) => {
          const ticketId = +Object.keys(ticketTypesMap).find(key => ticketTypesMap[key] === type)!;
          return {
            id: ticketId,
            type,
            price,
            quantity: 0
          };
        });

        this.eventData = {
          id: event.eventId,
          eventName: event.eventName,
          description: event.description,
          startTime: event.startTime,
          backgroundUrl: event.backgroundUrl,
          organizerName: event.organizerName,
          organizerBio: event.organizerBio,
          organizerAvatarUrl: event.organizerAvatarUrl,
          address: event.address,
          price,
          ticketPrices,
        };
      },
      error: (err) => {
        console.error('Lỗi khi lấy chi tiết sự kiện:', err);
      }
    });
  }


  increaseQuantity(ticket: any): void {
    const index = this.eventData.ticketPrices.findIndex((t: any) => t.type === ticket.type);
    if (index !== -1 && this.eventData.ticketPrices[index].quantity < 5) {
      this.eventData.ticketPrices[index].quantity++;
    }
  }

  decreaseQuantity(ticket: any): void {
    const index = this.eventData.ticketPrices.findIndex((t: any) => t.type === ticket.type);
    if (index !== -1 && this.eventData.ticketPrices[index].quantity > 0) {
      this.eventData.ticketPrices[index].quantity--;
    }
  }

  get totalAmount(): number {
    return (this.eventData.ticketPrices || []).reduce(
      (total: number, ticket: any) => total + ticket.quantity * ticket.price, 0
    );
  }

  get hasSelection(): boolean {
    return (this.eventData.ticketPrices || []).some((ticket: any) => ticket.quantity > 0);
  }

  get totalTicketCount(): number {
    return (this.eventData.ticketPrices || []).reduce((sum: number, t: any) => sum + t.quantity, 0);
  }

  proceedToOrder() {
    const selectedTickets = this.eventData.ticketPrices
      .filter((t: any) => t.quantity > 0)
      .map((t: any) => ({
        ticketId: t.id,
        type: t.type,
        price: t.price,
        quantity: t.quantity,
        total: t.price * t.quantity
      }));

    const totalAmount = selectedTickets.reduce((acc: number, item: any) => acc + item.total, 0);

    const orderData = {
      eventId: this.eventData.id,
      tickets: selectedTickets,
      totalAmount: totalAmount
      // discountCode: "ABC"
    };

    this.ticketOrderService.setOrder(orderData);
    this.router.navigate(['/order-ticket']);
  }

  backToDetails(eventId: number): void {
    this.router.navigate(['/detail-ticket', eventId]);
  }

  onNotificationClose() {}
}
