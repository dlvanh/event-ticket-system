// toast-notification.component.ts
import { Component, 
Input, 
Output, 
EventEmitter, 
OnInit, 
OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-toast-notification',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './toast-notification.component.html',
  styleUrls: ['./toast-notification.component.css']
})
export class ToastNotificationComponent implements OnInit, OnDestroy {
  @Input() message: string = 'Bạn phải đăng nhập để sử dụng chức năng này.';
  @Input() autoHideTime: number = 5000; // 5 giây
  @Input() show: boolean = false;
  @Input() showProgressBar: boolean = true; // Cho phép ẩn/hiện progress bar
  @Output() onClose = new EventEmitter<void>();
  progressBarType?: string;

  private timeoutId: any;

  ngOnInit() {
    if (this.show && this.autoHideTime > 0) {
      this.startAutoHide();
    }
  }

  ngOnDestroy() {
    this.clearAutoHide();
  }

  ngOnChanges() {
    if (this.show && this.autoHideTime > 0) {
      this.startAutoHide();
    } else {
      this.clearAutoHide();
    }
  }

  private startAutoHide() {
    this.clearAutoHide();
    this.timeoutId = setTimeout(() => {
      this.closeNotification();
    }, this.autoHideTime);
  }

  private clearAutoHide() {
    if (this.timeoutId) {
      clearTimeout(this.timeoutId);
      this.timeoutId = null;
    }
  }

  closeNotification() {
    this.show = false;
    this.clearAutoHide();
    this.onClose.emit();
  }

  // Method để show notification từ bên ngoài
  showNotification(message?: string, autoHideTime?: number, progressBarType?: string) {
    if (message) {
      this.message = message;
    }
    if (autoHideTime !== undefined) {
      this.autoHideTime = autoHideTime;
    }
    if (progressBarType) {
      this.progressBarType = progressBarType;
    }
    this.show = true;
    if (this.autoHideTime > 0) {
      this.startAutoHide();
    }
  }
}