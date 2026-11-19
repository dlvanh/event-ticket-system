import { Component, OnInit, AfterViewInit, ViewChild, ElementRef } from '@angular/core';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { Location, CommonModule } from '@angular/common';
import { ListEventsService } from '../../services/list-events.service';
import Chart from 'chart.js/auto';
import ChartDataLabels from 'chartjs-plugin-datalabels';

interface EventData {
  id: number;
  eventName: string;
  startTime: string;
  endTime: string;
  address: string;
  category: string;
  description: string;
}

interface TicketTypeStats {
  ticketType: string;
  totalQuantity: number;
  soldQuantity: number;
  remainingQuantity: number;
  price: number;
  revenue: number;
}

interface TicketStats {
  totalTickets: number;
  soldTickets: number;
  remainingTickets: number;
  totalRevenue: number;
  ticketTypes: TicketTypeStats[];
  revenue?: number;
}

@Component({
  selector: 'app-export-file-detail',
  templateUrl: './export-file-detail.component.html',
  styleUrls: ['./export-file-detail.component.css'],
  standalone: true,
  imports: [CommonModule, RouterModule]
})
export class ExportFileDetailComponent implements OnInit, AfterViewInit {
  @ViewChild('pieChartCanvas', { static: false }) pieChartCanvas!: ElementRef<HTMLCanvasElement>;
  pieChart: Chart | null = null;
  eventId!: number;
  eventData: EventData | null = null;

  ticketStats: TicketStats = {
    totalTickets: 0,
    soldTickets: 0,
    remainingTickets: 0,
    totalRevenue: 0,
    ticketTypes: []
  };

  isLoading = true;
  isLoadingStats = true;
  isDownloadingPdf = false;
  isDownloadingExcel = false;
  isDownloadingBuyerPdf = false;
  isDownloadingBuyerExcel = false;

  constructor(
    private route: ActivatedRoute,
    private location: Location,
    private listEventsService: ListEventsService
  ) {}

  ngOnInit(): void {
    this.eventId = Number(this.route.snapshot.paramMap.get('id'));
    if (!this.eventId) {
      this.isLoading = false;
      this.isLoadingStats = false;
      return;
    }

    this.loadEventAndStatistics();
  }

  ngAfterViewInit(): void {
    if (!this.isLoadingStats && this.ticketStats.ticketTypes.length > 0) {
      this.renderPieChart();
    }
  }

  goBack(): void {
    this.location.back();
  }

  private loadEventAndStatistics(): void {
    this.listEventsService.getEventById(this.eventId).subscribe({
      next: res => {
        const data = res?.data;
        this.eventData = data || null;
        this.isLoading = false;

        const ticketTypesStats: TicketTypeStats[] = [];
        if (data?.ticketTypes && data?.ticketPrices && data?.ticketsTotal && data?.ticketsSold) {
          Object.keys(data.ticketTypes).forEach(key => {
            const ticketType = data.ticketTypes[key];
            const price = Number(data.ticketPrices[ticketType] || 0);
            const totalQuantity = Number(data.ticketsTotal[ticketType] || 0);
            const soldQuantity = Number(data.ticketsSold[ticketType] || 0);

            ticketTypesStats.push({
              ticketType,
              totalQuantity,
              soldQuantity,
              remainingQuantity: totalQuantity - soldQuantity,
              price,
              revenue: soldQuantity * price
            });
          });
        }

        const totalTickets = ticketTypesStats.reduce((sum, t) => sum + t.totalQuantity, 0);
        const soldTickets = ticketTypesStats.reduce((sum, t) => sum + t.soldQuantity, 0);
        const totalRevenue = ticketTypesStats.reduce((sum, t) => sum + t.soldQuantity * t.price, 0);

        this.ticketStats = {
          totalTickets,
          soldTickets,
          remainingTickets: totalTickets - soldTickets,
          totalRevenue,
          ticketTypes: ticketTypesStats
        };

        this.isLoadingStats = false;
        setTimeout(() => this.renderPieChart(), 0);
      },
      error: err => {
        console.error('Lỗi khi tải thông tin & thống kê sự kiện:', err);
        this.eventData = null;
        this.isLoading = false;
        this.isLoadingStats = false;
      }
    });
  }

  renderPieChart(): void {
    if (!this.pieChartCanvas) return;
    const ctx = this.pieChartCanvas.nativeElement.getContext('2d');
    if (!ctx) return;

    if (this.pieChart) {
      this.pieChart.destroy();
    }

    const labels = this.ticketStats.ticketTypes.map(t => t.ticketType);
    const data = this.ticketStats.ticketTypes.map(t => t.soldQuantity);
    const backgroundColors = [
      '#ff7e42', '#3498db', '#2ecc71', '#e74c3c',
      '#9b59b6', '#f1c40f', '#1abc9c', '#34495e'
    ];

    this.pieChart = new Chart(ctx, {
      type: 'bar',
      data: {
        labels,
        datasets: [{
          label: 'Số vé đã bán',
          data,
          backgroundColor: backgroundColors.slice(0, labels.length),
          borderRadius: 6,
          barThickness: 40
        }]
      },
      options: {
        responsive: true,
        plugins: {
          legend: { display: false },
          tooltip: {
            callbacks: {
              label: (context: any) => `${context.label}: ${context.parsed.y} vé`
            }
          },
          datalabels: {
            anchor: 'end',
            align: 'start',
            color: '#ffffffff',
            font: { weight: 'bold', size: 13 },
            formatter: (value: number, context: any) => {
              const dataset = context.chart.data.datasets[0].data;
              const totalSold = dataset.reduce((a: number, b: number) => a + b, 0);
              const percent = totalSold ? ((value / totalSold) * 100).toFixed(1) : 0;
              return `${percent}%`;
            }
          }
        },
        scales: {
          x: {
            ticks: { color: '#fff' },
            grid: { color: '#444' }
          },
          y: {
            beginAtZero: true,
            max: data.reduce((a, b) => a + b, 0) || undefined,
            ticks: { color: '#fff' },
            grid: { color: '#444' }
          }
        }
      },
      plugins: [ChartDataLabels]
    });
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND'
    }).format(amount);
  }

  downloadPdfReport(): void {
    this.downloadReport('pdf');
  }

  downloadExcelReport(): void {
    this.downloadReport('excel');
  }

  private downloadReport(type: 'pdf' | 'excel'): void {
    const mimeType =
      type === 'pdf'
        ? 'application/pdf'
        : 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';
    const fileExt = type === 'pdf' ? 'pdf' : 'xlsx';

    if (type === 'pdf') this.isDownloadingPdf = true;
    if (type === 'excel') this.isDownloadingExcel = true;

    this.listEventsService.exportEventReport(this.eventId, type).subscribe({
      next: data => {
        this.triggerDownload(new Blob([data], { type: mimeType }), `event_report_${this.eventId}.${fileExt}`);
        if (type === 'pdf') this.isDownloadingPdf = false;
        if (type === 'excel') this.isDownloadingExcel = false;
      },
      error: err => {
        console.error(`Lỗi khi tải báo cáo ${type.toUpperCase()}:`, err);
        if (type === 'pdf') this.isDownloadingPdf = false;
        if (type === 'excel') this.isDownloadingExcel = false;
      }
    });
  }

  downloadBuyerReport(type: 'pdf' | 'excel'): void {
    const mimeType =
      type === 'pdf'
        ? 'application/pdf'
        : 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';
    const fileExt = type === 'pdf' ? 'pdf' : 'xlsx';

    if (type === 'pdf') this.isDownloadingBuyerPdf = true;
    if (type === 'excel') this.isDownloadingBuyerExcel = true;

    this.listEventsService.exportBuyerReport(this.eventId, type).subscribe({
      next: data => {
        this.triggerDownload(new Blob([data], { type: mimeType }), `buyers_event_${this.eventId}.${fileExt}`);
        if (type === 'pdf') this.isDownloadingBuyerPdf = false;
        if (type === 'excel') this.isDownloadingBuyerExcel = false;
      },
      error: err => {
        console.error(`Lỗi khi tải danh sách người mua (${type.toUpperCase()}):`, err);
        if (type === 'pdf') this.isDownloadingBuyerPdf = false;
        if (type === 'excel') this.isDownloadingBuyerExcel = false;
      }
    });
  }

  private triggerDownload(blob: Blob, filename: string): void {
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    window.URL.revokeObjectURL(url);
  }
}