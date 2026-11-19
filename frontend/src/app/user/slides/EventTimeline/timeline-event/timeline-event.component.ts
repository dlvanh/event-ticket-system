import { Component } from '@angular/core';
import { ThismonthEventComponent } from '../tabs/thismonth-event/thismonth-event.component';
import { ThisweekendEventComponent } from '../tabs/thisweekend-event/thisweekend-event.component';
import { CommonModule } from '@angular/common';
import { EventsService } from '../../../services/events.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-timeline-event',
  imports: [ThismonthEventComponent,ThisweekendEventComponent, CommonModule],
  templateUrl: './timeline-event.component.html',
  styleUrl: './timeline-event.component.css'
})
export class TimelineEventComponent {
  images: string[] = [
    '/assets/anh.webp'
  ];

  selectedTab: string = 'thisweekend';
  setTab(tab: string) {
    this.selectedTab = tab;
  }
  
  constructor(private eventService: EventsService,private router: Router) {}

  onSeeMore() {
  console.log('Xem thêm được bấm từ tab:', this.selectedTab);
  
  // Xử lý logic dựa trên tab hiện tại
  switch(this.selectedTab) {
    case 'thisweekend':
      this.loadMoreWeekendEvents();
      break;
    case 'thismonth':
      this.loadMoreMonthEvents();
      break;
  }
}

  private loadMoreWeekendEvents() {
    // Lấy thời gian cuối tuần
    const weekendRange = this.getWeekendTimeRange();
    
    // Navigate với queryParams cho cuối tuần
    this.router.navigate(['/search-events'], {
      queryParams: { 
        tab: 'thisweekend',
        startTime: weekendRange.startTimeISO,
        endTime: weekendRange.endTimeISO
      }
    });
    
    this.eventService.searchEvents({
      startTime: weekendRange.startTimeISO
      // các params khác nếu cần (category, address)
    }).subscribe({
      next: (response) => {
        console.log('Dữ liệu cuối tuần:', response);
        // Xử lý dữ liệu cuối tuần (append vào list hiện tại, etc.)
      },
      error: (error) => {
        console.error('Lỗi khi tải dữ liệu cuối tuần:', error);
      }
    });
  }

  private loadMoreMonthEvents() {
    // Lấy thời gian tháng này
    const monthRange = this.getMonthTimeRange();
    
    // Navigate với queryParams cho tháng này
    this.router.navigate(['/search-events'], {
      queryParams: { 
        tab: 'thismonth',
        startTime: monthRange.startTimeISO,
        endTime: monthRange.endTimeISO
      }
    });
    
    this.eventService.searchEvents({
      startTime: monthRange.startTimeISO,
      endTime: monthRange.endTimeISO
      // các params khác nếu cần (category, address)
    }).subscribe({
      next: (response) => {
        console.log('Dữ liệu tháng này:', response);
        // Xử lý dữ liệu tháng này (append vào list hiện tại, etc.)
      },
      error: (error) => {
        console.error('Lỗi khi tải dữ liệu tháng này:', error);
      }
    });
  }

  // Method cho cuối tuần (thứ 7 + chủ nhật)
  getWeekendTimeRange(): { startTimeISO: string, endTimeISO: string, startDate: Date, endDate: Date } {
    const now = new Date();
    const currentDayOfWeek = now.getDay();
    
    let daysToSaturday: number;
    if (currentDayOfWeek === 0) { 
      daysToSaturday = 6; 
    } else {
      daysToSaturday = 6 - currentDayOfWeek; 
    }
    
    // Thứ 7 tuần này từ 00:00:00
    const startDate = new Date(now);
    startDate.setDate(now.getDate() + daysToSaturday);
    startDate.setHours(0, 0, 0, 0);
    
    // Chủ nhật tuần này đến 23:59:59
    const endDate = new Date(startDate);
    endDate.setDate(startDate.getDate() + 1); // Chủ nhật
    endDate.setHours(23, 59, 59, 999);
    
    const startTimeISO = this.toISOWithTimezoneOffset(startDate, 7);
    const endTimeISO = this.toISOWithTimezoneOffset(endDate, 7);
    
    return { startTimeISO, endTimeISO, startDate, endDate };
  }

  // Method cho tháng này
  getMonthTimeRange(): { startTimeISO: string, endTimeISO: string, startDate: Date, endDate: Date } {
    const now = new Date();

    // Ngày bắt đầu tháng: 01/tháng hiện tại lúc 00:00
    const startDate = new Date(now.getFullYear(), now.getMonth(), 1, 0, 0, 0, 0);

    // Ngày kết thúc tháng: ngày cuối cùng trong tháng lúc 23:59:59.999
    const endDate = new Date(now.getFullYear(), now.getMonth() + 1, 0, 23, 59, 59, 999);

    const startTimeISO = this.toISOWithTimezoneOffset(startDate, 7);
    const endTimeISO = this.toISOWithTimezoneOffset(endDate, 7);

    return { startTimeISO, endTimeISO, startDate, endDate };
  }

  // Helper method để convert sang ISO với timezone offset
  toISOWithTimezoneOffset(date: Date, offsetHours: number): string {
    const offsetDate = new Date(date.getTime() - offsetHours * 60 * 60 * 1000);
    return offsetDate.toISOString().replace('Z', `+${offsetHours.toString().padStart(2, '0')}:00`);
  }

  // Helper method để format date
  formatDate(isoDate: string): string {
    const date = new Date(isoDate);
    const day = date.getDate().toString().padStart(2, '0');
    const month = (date.getMonth() + 1).toString().padStart(2, '0');
    const year = date.getFullYear();
    return `${day} tháng ${month}, ${year}`;
  }

  // Method để filter events theo thời gian (nếu cần dùng ở client side)
  filterEventsByTimeRange(allEvents: any[], timeRange: 'weekend' | 'month') {
    let startDate: Date, endDate: Date;
    
    if (timeRange === 'weekend') {
      const weekendRange = this.getWeekendTimeRange();
      startDate = weekendRange.startDate;
      endDate = weekendRange.endDate;
    } else {
      const monthRange = this.getMonthTimeRange();
      startDate = monthRange.startDate;
      endDate = monthRange.endDate;
    }
    
    const filteredEvents = allEvents.filter((event: any) => {
      const raw = event.startTime.replace(' ', 'T') + '+07:00';
      const eventTime = new Date(raw);
      
      const isInRange = eventTime.getTime() >= startDate.getTime() && eventTime.getTime() <= endDate.getTime();
      return isInRange;
    });
    
    return filteredEvents;
  }
}
