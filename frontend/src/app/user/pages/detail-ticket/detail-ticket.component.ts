import { Component, OnInit, HostListener,ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule, NgIf } from '@angular/common';
import { AuthService } from '../../../auth/services/auth.service';
import { ToastNotificationComponent } from '../../pop-up/toast-notification/toast-notification.component';
import { ActivatedRoute } from '@angular/router';
import { EventsService } from '../../services/events.service';
import { UserService } from '../../services/user.service';

@Component({
  selector: 'app-detail-ticket',
  imports: [CommonModule, ToastNotificationComponent],
  templateUrl: './detail-ticket.component.html',
  styleUrl: './detail-ticket.component.css'
})
export class DetailTicketComponent implements OnInit {
  eventId!: number;
  eventData: any = {};
  ticketList: any[] = [];
  events: any[] = [];
  reviews: any[] = [];
  averageRating: number = 0;
  totalReviews: number = 0;
  stars: string[] = [];

  expanded = false;
  showHeader = false;
  isMainExpanded: boolean = true;
  @ViewChild('notification') notification!: ToastNotificationComponent;
  
  showNotification = false;
  notificationMessage = 'Bạn phải đăng nhập để sử dụng chức năng này.';

  get displayedTickets() {
    const tickets = this.eventData.ticketPrices || [];
    return this.expanded ? tickets : tickets.slice(0, 3);
  }

  toggleRows(): void {
    this.expanded = !this.expanded;
  }

  toggleTicket(index: number): void {
    this.eventData.ticketPrices[index].isExpanded = !this.eventData.ticketPrices[index].isExpanded;
  }

  constructor(private router: Router, private authService: AuthService, private route: ActivatedRoute,
    private eventsService: EventsService,
    private userService: UserService) { }

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      this.eventId = +params['id'];
      this.loadEventDetail(this.eventId);

      // Check để tránh lỗi trong SSR
      if (typeof window !== 'undefined') {
        window.scrollTo({ top: 0, behavior: 'smooth' });
      }
    });

    this.loadEvents();
    this.loadReviews(this.eventId);
  }

  loadEventDetail(id: number): void {
    this.eventsService.getEventById(id).subscribe({
      next: (res) => {
        const event = res?.data;

        if (this.eventHasEnded()) {
          this.loadReviews(id);
        }
        
        const ticketPricesObj = event.ticketPrices || {};
        const ticketsSoldObj = event.ticketsSold || {};
        const ticketsTotalObj = event.ticketsTotal || {};

        const ticketPrices = Object.entries(ticketPricesObj).map(([type, price]) => {
          const sold = ticketsSoldObj[type] || 0;
          const total = ticketsTotalObj[type] || 0;
          const available = total - sold;

          return {
            type,
            price,
            total,
            sold,
            available,
            isSoldOut: available <= 0
          };
        });

        const allSoldOut = ticketPrices.every(ticket => ticket.isSoldOut);

        const price = Math.min(...ticketPrices.map(v => Number(v.price))) || 0;

        const fullAddress = event.address || event.addressName || '';
        const parts = fullAddress.split(',').map((part: string) => part.trim());

        let addressDetail = '';
        let addressName = '';

        if (parts.length >= 3) {
          addressDetail = parts[1];
          addressName = [parts[0], ...parts.slice(2)].join(', ');
        } else {
          addressName = fullAddress;
        }

        this.eventData = {
          id: event.eventId,
          eventName: event.eventName,
          description: event.description,
          startTime: event.startTime,
          endTime: event.endTime,
          ticketsSaleStartTime: event.ticketsSaleStartTime,
          backgroundUrl: event.backgroundUrl,
          organizerName: event.organizerName,
          organizerBio: event.organizerBio,
          organizerAvatarUrl: event.organizerAvatarUrl,

          addressDetail: addressDetail,
          addressName: addressName,

          price,
          ticketPrices,
          allSoldOut,
          isExpanded: false
        };

      },
      error: (err) => {
        console.error('Lỗi khi lấy chi tiết sự kiện:', err);
      }
    });
  }

  loadEvents() {
    this.eventsService.getRecommendedEvents(
      '', 
      '', 
      '', 
      '', 
      '', 
      '',
      1, 
      12 
    ).subscribe(res => {
      this.events = res?.data?.listEvents || [];
      // Map lại nếu cần, đảm bảo có đủ các trường
      this.events = this.events.map((event: any) => ({
        id: event.eventId,
        eventName: event.eventName,
        description: event.description,
        startTime: event.startTime,
        endTime: event.endTime,
        category: event.category,
        status: event.status,
        approval_status: event.approval_status,
        backgroundUrl: event.backgroundUrl,
        address: event.address || event.addressName,
        addressDetail: event.addressDetail,
        price: event.minPrice
      }));
    });
  }

  promotionalBannerUrl = 'https://storage.googleapis.com/a1aa/image/42737dff-09dd-4692-9d1c-4f174cd415e5.jpg';

  onLoadMoreEvents(){
    this.router.navigate(['/home']);
  }

  onEventClick(eventId: number) {
    this.router.navigateByUrl('/', { skipLocationChange: true }).then(() => {
      this.router.navigate(['/detail-ticket', eventId]);
      window.scrollTo({ top: 0, behavior: 'smooth' });
    });
  }


  @HostListener('window:scroll', [])
  onWindowScroll() {
    const scrollPosition = window.pageYOffset || document.documentElement.scrollTop || document.body.scrollTop || 0;
    this.showHeader = scrollPosition > 600;
  }

  toggleMainSection(): void {
    this.isMainExpanded = !this.isMainExpanded;
  }

  isLoggedIn(): boolean {
    return this.authService.isLoggedIn();
  }

  needLoginToBuyTicket() {
    if (this.isLoggedIn()) {
      this.router.navigate(['/slider-captcha'], {
        queryParams: { eventId: this.eventId } 
      });
    } else {
      this.notification.showNotification(
        'Vui lòng đăng nhập để mua vé sự kiện này !',
        5000,
        "warning",
      );
    }
  }

  isAllTicketsSoldOut(): boolean {
    return this.eventData.ticketPrices?.every((ticket: any) => ticket.isSoldOut);
  }

  eventHasEnded(): boolean {
    if (!this.eventData?.endTime) return false;

    const now = new Date();

    let end = new Date(this.eventData.endTime);

    // Nếu bạn chắc chắn endTime là UTC, và muốn xét theo giờ VN (UTC+7):
    end = new Date(end.getTime() + 7 * 60 * 60 * 1000);

    return now > end;
  }


  canBuyTicket(): boolean {
    const now = new Date();
    const startTime = new Date(this.eventData.ticketsSaleStartTime);
    return now >= startTime;
  }


  getGoogleMapUrl(addressDetail: string, addressName: string): string {
    const fullAddress = `${addressDetail}, ${addressName}`;
    return `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(fullAddress)}`;
  }

  loadReviews(id: number) {
    this.userService.getReviewsByEvent(id).subscribe({
      next: (res) => {
        this.reviews = res?.data.reviewDetails;

        if (this.reviews.length > 0) {
          this.totalReviews = this.reviews.length;
          const sum = this.reviews.reduce((acc, review) => acc + review.rating, 0);
          this.averageRating = Math.round((sum / this.totalReviews) * 10) / 10;

          this.generateStars();
        } else {
          this.totalReviews = 0;
          this.averageRating = 0;
          this.stars = [];
        }
      },
      error: (err) => {
        // console.error('Lỗi khi lấy review:', err);
      }
    });
  }

  generateStars() {
    this.stars = [];
    for (let i = 1; i <= 5; i++) {
      if (i <= Math.floor(this.averageRating)) {
        this.stars.push('full');
      } else if (i - this.averageRating < 1 && this.averageRating % 1 >= 0.5) {
        this.stars.push('half');
      } else {
        this.stars.push('empty');
      }
    }
  }

  goToReview(eventId: number) {
    if(!this.eventHasEnded()){
      this.notification.showNotification(
        'Sự kiện chưa kết thúc. Chưa thể đánh giá sự kiện !',
        5000,
        "warning",
      );
    }
    else{
      this.router.navigate(['/review-ticket', eventId]);
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  }
  
  onNotificationClose() {}
}
