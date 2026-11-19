import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { UserService } from '../../user/services/user.service';
import { AuthService } from '../../auth/services/auth.service';
import { CommonModule} from '@angular/common';
import { Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common'

@Component({
  selector: 'app-nav-user',
  imports: [CommonModule],
  templateUrl: './nav-user.component.html',
  styleUrl: './nav-user.component.css'
})
export class NavUserComponent {
  avatarUrl: string = '';
  id: number = 0;

  constructor(private router: Router,private userService: UserService, 
    private authService: AuthService,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit() {
    if (this.authService.isLoggedIn()) {
      this.userService.getAvatar().subscribe(url => {
        this.avatarUrl = url;
      });
    } else {}
  }

  navigateToHome() {
    this.router.navigate(['/home']);
  }

  navigateToProfile() {
    this.router.navigate(['/user-profile']);
  }

  navigateToPurchasedTickets() {
    if (isPlatformBrowser(this.platformId)) {
      const userId = localStorage.getItem('userId');
      if (userId) {
        this.router.navigate([`/purchased-ticket/${userId}`]);
      } else {
        console.error('userId not found in localStorage');
      }
    }
  }

  navigateToSelectTicket() {
    this.router.navigate(['/select-ticket']);
  }
  
  getFullName(): string | null {
    return this.userService.getFullName();
  }
}
