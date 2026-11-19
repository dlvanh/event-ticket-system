import { Component, OnInit, OnDestroy, Inject, PLATFORM_ID, ViewChild } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { TicketOrderService } from '../../services/ticket-order.service';
import { CommonModule } from '@angular/common';
import { LoadingComponent } from '../../pop-up/loading/loading.component';
import { ToastNotificationComponent } from '../../pop-up/toast-notification/toast-notification.component';
import { Router } from '@angular/router';
import { EventsService } from '../../services/events.service';
import { FormsModule } from '@angular/forms';
import Swal from 'sweetalert2';
import { CanComponentDeactivate } from '../../../auth/services/pending-changes.guard';
import { UserService } from '../../services/user.service';

@Component({
  selector: 'app-order-ticket',
  imports: [ CommonModule, LoadingComponent, ToastNotificationComponent, FormsModule],
  templateUrl: './order-ticket.component.html',
  styleUrl: './order-ticket.component.css'
})
export class OrderTicketComponent implements OnInit, OnDestroy, CanComponentDeactivate {

  async canDeactivate(): Promise<boolean> {
    if (this.forceNavigate) {
      return true; // Bỏ qua Swal nếu đang tự động chuyển trang
    }

    const result = await Swal.fire({
      title: 'Bạn có chắc muốn rời khỏi trang?',
      text: 'Dữ liệu đặt vé sẽ bị huỷ!',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Rời khỏi',
      cancelButtonText: 'Ở lại',
      reverseButtons: true,
      customClass: {
        popup: 'my-swal-popup',
        title: 'my-swal-title',
        htmlContainer: 'my-swal-text',
        confirmButton: 'my-swal-confirm',
        cancelButton: 'my-swal-cancel'
      }
    });

    if (result.isConfirmed) {
      sessionStorage.removeItem('orderData');
      sessionStorage.removeItem(this.STORAGE_KEY);
      clearInterval(this.interval);
      this.resetTimer();
      return true;
    }

    return false;
  }

  forceNavigate = false;
  orderData: any;
  isLoading: boolean = true;
  isDiscountValid: boolean = false;
  @ViewChild('notification') notification!: ToastNotificationComponent;
  showNotification = false;
  eventDetail: any;
  discountAmount: number = 0;
  discountCode: string = '';

  minutes: string = '15';
  seconds: string = '00';
  private interval: any;
  private readonly STORAGE_KEY = 'countdown-timer';
  private readonly INITIAL_TIME = 15 * 60; // 15 phút
  private isBrowser: boolean;

  constructor(private router: Router, @Inject(PLATFORM_ID) private platformId: Object,
    private ticketOrderService: TicketOrderService,
    private eventsService: EventsService,
    private userService: UserService) {
    this.isBrowser = isPlatformBrowser(this.platformId);
  }

  ngOnInit() {
    this.initializeTimer();
    this.startTimer();

    setTimeout(() => {
      this.orderData = this.ticketOrderService.getOrder();

      if (!this.orderData) {
        if (!this.isBrowser) return;
        const orderDataString = sessionStorage.getItem('orderData');
        if (orderDataString) {
          try {
            this.orderData = JSON.parse(orderDataString);
          } catch (error) {
            console.error('Lỗi parse orderData từ sessionStorage:', error);
          }
        }
      }

      const eventId = this.orderData?.eventId;
      if (eventId) {
        this.loadEventDetail(eventId);
      }

      this.isLoading = false;
    }, 100);
  }


  ngOnDestroy() {
    if (this.interval) {
      clearInterval(this.interval);
    }
  }

  private initializeTimer() {
    if (!this.isBrowser) {
      this.updateDisplay(this.INITIAL_TIME);
      return;
    }

    const savedData = sessionStorage.getItem(this.STORAGE_KEY);
    
    if (savedData) {
      const { endTime } = JSON.parse(savedData);
      const now = Date.now();
      
      if (now < endTime) {
        // Timer vẫn còn hiệu lực
        const remainingTime = Math.ceil((endTime - now) / 1000);
        this.updateDisplay(remainingTime);
      } else {
        // Timer đã hết hạn
        this.resetTimer();
      }
    } else {
      // Lần đầu tiên vào trang trong session
      this.resetTimer();
    }
  }

  private resetTimer() {
    if (!this.isBrowser) return;
    
    const endTime = Date.now() + (this.INITIAL_TIME * 1000);
    sessionStorage.setItem(this.STORAGE_KEY, JSON.stringify({ endTime }));
    this.updateDisplay(this.INITIAL_TIME);
  }

  private startTimer() {
    if (!this.isBrowser) return;
    
    this.interval = setInterval(() => {
      const savedData = sessionStorage.getItem(this.STORAGE_KEY);
      
      if (savedData) {
        const { endTime } = JSON.parse(savedData);
        const now = Date.now();
        const remainingTime = Math.ceil((endTime - now) / 1000);
        
        if (remainingTime > 0) {
          this.updateDisplay(remainingTime);
        } else {
          // Timer đã hết
          this.updateDisplay(0);
          clearInterval(this.interval);
          this.onTimerEnd();
        }
      }
    }, 1000);
  }

  private updateDisplay(totalSeconds: number) {
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;
    
    this.minutes = minutes.toString().padStart(2, '0');
    this.seconds = seconds.toString().padStart(2, '0');
  }

  private onTimerEnd() {
    if (this.isBrowser) {
      sessionStorage.removeItem(this.STORAGE_KEY);
    }

    const eventId = this.orderData?.eventId; 

    this.notification.showNotification('Hết thời gian đặt vé. Vui lòng chọn lại vé !', 3000, 'warning');

    setTimeout(() => {
      if (eventId) {
        this.forceNavigate = true;
        this.router.navigate(['/select-ticket', eventId]);
      }
    }, 3000);
  } 

  loadEventDetail(eventId: number) {
    this.eventsService.getEventById(eventId).subscribe({
      next: (res) => {
        this.eventDetail = res.data; // <- Chỉ lấy phần 'data'
      },
      error: (err) => {
        console.error('Lỗi khi load chi tiết sự kiện:', err);
      }
    });
  }

  applyDiscount() {
    const codeInput = this.discountCode.trim();

    if (!codeInput) {
      this.discountAmount = 0;
      this.isDiscountValid = false;
      this.notification.showNotification('Vui lòng nhập mã giảm giá!', 3000, 'warning');
      return;
    }

    this.userService.getDiscountByCode(codeInput).subscribe({
      next: (res) => {
        const discount = res?.data;
        const orderEventId = this.orderData?.eventId;
        const discountEventId = discount?.event?.eventId;

        if (!orderEventId || !discountEventId || orderEventId !== discountEventId) {
          this.discountAmount = 0;
          this.isDiscountValid = false;
          this.notification.showNotification('Mã giảm giá không áp dụng cho sự kiện này.', 3000, 'warning');
          return;
        }

        const now = new Date();
        const validFrom = new Date(discount.validFrom);
        const validTo = new Date(discount.validTo);

        if (now < validFrom) {
          this.discountAmount = 0;
          this.isDiscountValid = false;
          this.notification.showNotification('Mã giảm giá chưa bắt đầu áp dụng.', 3000, 'warning');
          return;
        }

        if (now > validTo) {
          this.discountAmount = 0;
          this.isDiscountValid = false;
          this.notification.showNotification('Mã giảm giá đã hết hạn. Vui lòng chọn mã khác!', 3000, 'warning');
          return;
        }

        if (discount.maxUsage == 0) {
          this.discountAmount = 0;
          this.isDiscountValid = false;
          this.notification.showNotification('Mã giảm giá đã hết lượt sử dụng.', 3000, 'warning');
          return;
        }

        const total = this.orderData?.totalAmount || 0;

        if (discount.discountType === 'percentage') {
          this.discountAmount = Math.floor((discount.value / 100) * total);
        } else if (discount.discountType === 'fixed_amount') {
          this.discountAmount = discount.value;
        }

        this.isDiscountValid = true;

        this.notification.showNotification(
          `Đã áp dụng mã. Giảm ${this.discountAmount.toLocaleString()} đ`,
          3000,
          'success'
        );
      },
      error: () => {
        this.discountAmount = 0;
        this.isDiscountValid = false;
        this.notification.showNotification('Mã giảm giá không hợp lệ.', 3000, 'error');
      }
    });
  }

  getFinalAmount(): number {
    const total = this.orderData?.totalAmount || 0;
    return Math.max(total - this.discountAmount, 0);
  }

  submitOrder() {
    const paymentData = {
      ...this.orderData,
      discountCode: this.discountCode,
      returnUrl: 'https://url.ngrok-free.app/success',
      cancelUrl: 'https://url.ngrok-free.app/cancel'
    };

    this.ticketOrderService.payOrder(paymentData).subscribe({
      next: (res: any) => {
        const checkoutUrl = res?.data?.checkoutUrl;
        if (checkoutUrl) {
          this.notification.showNotification('Đang chuyển hướng đến trang thanh toán...', 3000, 'info');
          window.location.href = checkoutUrl;
        } else {
          this.notification.showNotification('Không tìm thấy liên kết thanh toán!', 3000, 'error');
        }
      },
      error: (err) => {
        console.error('Lỗi đặt vé:', err);
        this.notification.showNotification('Không thể đặt vé. Vui lòng thử lại.', 3000, 'error');
      }
    });
  }

  onNotificationClose() {}
}
