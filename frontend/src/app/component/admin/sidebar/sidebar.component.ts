import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router'; // Nhập RouterModule
import { AuthService } from '../../../auth/services/auth.service';


@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule], // Thêm RouterModule vào imports
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.css'], // Sửa styleUrl thành styleUrls
})
export class SidebarComponent {
  constructor(private authService: AuthService) {}

  logout() {
    this.authService.logout();
    window.location.href = '/home';
  }
}
