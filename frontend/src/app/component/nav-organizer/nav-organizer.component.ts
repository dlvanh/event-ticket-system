import { Component, OnInit } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { filter } from 'rxjs/operators';

@Component({
  selector: 'app-nav-organizer',
  standalone: true,
  imports: [
    RouterModule,
    CommonModule,
  ],
  templateUrl: './nav-organizer.component.html',
  styleUrls: ['./nav-organizer.component.css']
})
export class NavOrganizerComponent implements OnInit {
  currentRoute: string = '';
  isLoggedIn: boolean = false;
  isLogoutModalOpen: boolean = false;
  notificationCount: number = 2;
  currentDate: Date = new Date();

  constructor(private router: Router) {}

  ngOnInit() {
    this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe((event: NavigationEnd) => {
        this.currentRoute = event.urlAfterRedirects;
      });
  }

  logout() {
  // Xóa token hoặc session
  localStorage.removeItem('token');
  // Điều hướng về trang đăng nhập
  this.router.navigate(['/login']);
  // Hoặc thêm đoạn gọi API logout nếu cần
}

  goToEvents(): void {
    this.router.navigate(['/organizer/events']);
  }

  goToExportFile(): void {
    this.router.navigate(['/organizer/export-file']);
  }

  goToLegalDocument(): void {
    this.router.navigate(['/organizer/legal-document']);
  }

  goToCreateEvent(): void {
    this.router.navigate(['/organizer/create-event']);
  }

  goToProfile(): void {
    this.router.navigate(['/organizer/organizer-profile']);
  }

  goToReviewEvent(): void {
    this.router.navigate(['/organizer/review-event']);
  }
}
