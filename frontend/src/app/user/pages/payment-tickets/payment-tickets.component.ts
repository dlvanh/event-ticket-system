import { Component, OnInit, OnDestroy, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

@Component({
  selector: 'app-payment-tickets',
  imports: [],
  templateUrl: './payment-tickets.component.html',
  styleUrl: './payment-tickets.component.css'
})
export class PaymentTicketsComponent implements OnInit, OnDestroy {
  minutes: string = '15';
  seconds: string = '00';
  private interval: any;
  private readonly STORAGE_KEY = 'countdown-timer';
  private readonly INITIAL_TIME = 15 * 60; // 15 phút
  private isBrowser: boolean;

  constructor(@Inject(PLATFORM_ID) private platformId: Object) {
    this.isBrowser = isPlatformBrowser(this.platformId);
  }

  ngOnInit() {
    this.initializeTimer();
    this.startTimer();
  }

  ngOnDestroy() {
    if (this.interval) {
      clearInterval(this.interval);
    }
  }

  private initializeTimer() {
    if (!this.isBrowser) {
      // Nếu không phải browser (SSR), chỉ hiển thị thời gian ban đầu
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
    // Xử lý khi timer kết thúc
    console.log('Timer đã kết thúc!');
    // Có thể thêm logic redirect hoặc hiển thị thông báo
    if (this.isBrowser) {
      sessionStorage.removeItem(this.STORAGE_KEY);
    }
  }
}
