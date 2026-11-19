import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AdminService } from '../service/admin.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {
  userCount: number = 0;
  eventCount: number = 0;
  ticketCount: number = 0;
  isLoading: boolean = true;

  // Statistics for display
  eventStats = { pending: 0, approved: 0, rejected: 0, pendingPercent: 0, approvedPercent: 0, rejectedPercent: 0 };
  orderStats = { pending: 0, paid: 0, cancelled: 0, pendingPercent: 0, paidPercent: 0, cancelledPercent: 0 };
  ticketSalesTrend: { date: string; tickets: number }[] = [];

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    console.log('Dashboard component initialized');
    this.loadCounts();
  }

  loadCounts(): void {
    console.log('Starting to load counts...');
    let completedRequests = 0;
    const totalRequests = 6; // User count, event count, ticket count, event stats (3), order stats (3)

    // Load user count
    this.adminService.getUserCount().subscribe({
      next: (count) => {
        console.log('Received user count from service:', count);
        this.userCount = count;
        completedRequests++;
        this.checkLoadingState(completedRequests, totalRequests);
      },
      error: (error) => {
        console.error('Error loading user count:', error);
        completedRequests++;
        this.checkLoadingState(completedRequests, totalRequests);
      }
    });

    // Load event count
    this.adminService.getEventCount().subscribe({
      next: (count) => {
        console.log('Received event count from service:', count);
        this.eventCount = count;
        this.updateEventPercentages();
        completedRequests++;
        this.checkLoadingState(completedRequests, totalRequests);
      },
      error: (error) => {
        console.error('Error loading event count:', error);
        completedRequests++;
        this.checkLoadingState(completedRequests, totalRequests);
      }
    });

    // Load ticket count and ticket sales trend
    this.adminService.getOrders({}).subscribe({
      next: (response) => {
        if (response && response.data && response.data.listOrders) {
          this.ticketCount = response.data.listOrders.reduce((sum: number, order: any) => sum + (order.totalTicketsCount || 0), 0);
          console.log('Received ticket count from service:', this.ticketCount);

          // Calculate ticket sales trend (group by orderDate)
          const ticketSalesMap = new Map<string, number>();
          response.data.listOrders.forEach((order: any) => {
            const date = order.orderDate ? order.orderDate.split('T')[0] : 'Unknown';
            const tickets = order.totalTicketsCount || 0;
            ticketSalesMap.set(date, (ticketSalesMap.get(date) || 0) + tickets);
          });
          this.ticketSalesTrend = Array.from(ticketSalesMap.entries())
            .map(([date, tickets]) => ({ date, tickets }))
            .sort((a, b) => a.date.localeCompare(b.date));
          console.log('Ticket sales trend:', this.ticketSalesTrend);
        }
        completedRequests++;
        this.checkLoadingState(completedRequests, totalRequests);
      },
      error: (error) => {
        console.error('Error loading ticket count:', error);
        completedRequests++;
        this.checkLoadingState(completedRequests, totalRequests);
      }
    });

    // Load event statistics
    this.adminService.getEventCountByStatus('pending').subscribe({
      next: (count) => {
        this.eventStats.pending = count;
        this.updateEventPercentages();
        completedRequests++;
        this.checkLoadingState(completedRequests, totalRequests);
      },
      error: (error) => {
        console.error('Error loading pending events:', error);
        completedRequests++;
        this.checkLoadingState(completedRequests, totalRequests);
      }
    });
    this.adminService.getEventCountByStatus('approved').subscribe({
      next: (count) => {
        this.eventStats.approved = count;
        this.updateEventPercentages();
        completedRequests++;
        this.checkLoadingState(completedRequests, totalRequests);
      },
      error: (error) => {
        console.error('Error loading approved events:', error);
        completedRequests++;
        this.checkLoadingState(completedRequests, totalRequests);
      }
    });
    this.adminService.getEventCountByStatus('rejected').subscribe({
      next: (count) => {
        this.eventStats.rejected = count;
        this.updateEventPercentages();
        completedRequests++;
        this.checkLoadingState(completedRequests, totalRequests);
      },
      error: (error) => {
        console.error('Error loading rejected events:', error);
        completedRequests++;
        this.checkLoadingState(completedRequests, totalRequests);
      }
    });

    // Load order statistics
    this.adminService.getOrderCountByStatus('pending').subscribe({
      next: (count) => {
        this.orderStats.pending = count;
        this.updateOrderPercentages();
        completedRequests++;
        this.checkLoadingState(completedRequests, totalRequests);
      },
      error: (error) => {
        console.error('Error loading pending orders:', error);
        completedRequests++;
        this.checkLoadingState(completedRequests, totalRequests);
      }
    });
    this.adminService.getOrderCountByStatus('paid').subscribe({
      next: (count) => {
        this.orderStats.paid = count;
        this.updateOrderPercentages();
        completedRequests++;
        this.checkLoadingState(completedRequests, totalRequests);
      },
      error: (error) => {
        console.error('Error loading paid orders:', error);
        completedRequests++;
        this.checkLoadingState(completedRequests, totalRequests);
      }
    });
    this.adminService.getOrderCountByStatus('cancelled').subscribe({
      next: (count) => {
        this.orderStats.cancelled = count;
        this.updateOrderPercentages();
        completedRequests++;
        this.checkLoadingState(completedRequests, totalRequests);
      },
      error: (error) => {
        console.error('Error loading cancelled orders:', error);
        completedRequests++;
        this.checkLoadingState(completedRequests, totalRequests);
      }
    });
  }

  private updateEventPercentages(): void {
    const total = this.eventStats.pending + this.eventStats.approved + this.eventStats.rejected;
    if (total > 0) {
      this.eventStats.pendingPercent = (this.eventStats.pending / total) * 100;
      this.eventStats.approvedPercent = (this.eventStats.approved / total) * 100;
      this.eventStats.rejectedPercent = (this.eventStats.rejected / total) * 100;
    } else {
      this.eventStats.pendingPercent = 0;
      this.eventStats.approvedPercent = 0;
      this.eventStats.rejectedPercent = 0;
    }
  }

  private updateOrderPercentages(): void {
    const total = this.orderStats.pending + this.orderStats.paid + this.orderStats.cancelled;
    if (total > 0) {
      this.orderStats.pendingPercent = (this.orderStats.pending / total) * 100;
      this.orderStats.paidPercent = (this.orderStats.paid / total) * 100;
      this.orderStats.cancelledPercent = (this.orderStats.cancelled / total) * 100;
    } else {
      this.orderStats.pendingPercent = 0;
      this.orderStats.paidPercent = 0;
      this.orderStats.cancelledPercent = 0;
    }
  }

  private checkLoadingState(completed: number, total: number): void {
    if (completed >= total) {
      this.isLoading = false;
    }
  }
}
