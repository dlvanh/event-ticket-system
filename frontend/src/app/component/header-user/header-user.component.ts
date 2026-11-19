import { Component, HostListener, ElementRef } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule, NgIf } from '@angular/common';
import { AuthService } from '../../auth/services/auth.service';
import { UserService } from '../../user/services/user.service';
import { EventsService } from '../../user/services/events.service';
import { FormsModule } from '@angular/forms';
import { Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common'

@Component({
  selector: 'app-header-user',
  imports: [CommonModule, NgIf, FormsModule],
  templateUrl: './header-user.component.html',
  styleUrl: './header-user.component.css'
})
export class HeaderUserComponent {
  isDropdownOpen = false;
  avatarUrl: string = '';
  searchKeyword: string = '';
  id: number = 0;

  constructor(private router: Router, private authService: AuthService,
    private elementRef: ElementRef,
    private userService: UserService,
    private eventService: EventsService,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit() {
    if (this.authService.isLoggedIn()) {
      this.userService.getAvatar().subscribe(url => {
        this.avatarUrl = url;
      });
    } else {}
  }

  // Hàm tìm kiếm
  searchByEventName() {
    const trimmedKeyword = this.searchKeyword.trim();

    if (trimmedKeyword) {
      this.router.navigate(['/search-events'], {
        queryParams: { name: trimmedKeyword }
      });
    } else {
      this.router.navigate(['/search-events']);
    }
  }


  goToLogin() {
    this.router.navigate(['/login']);
  }

  goToRegister() {
    this.router.navigate(['/register']);
  }

  goToHome() {
    this.router.navigate(['/home']);
  }

  goToMyTickets() {
    this.router.navigate([`/purchased-ticket`]);
  }


  logout(){
    this.authService.logout();
    window.location.href = '/home';
  }

  isLoggedIn(): boolean {
    return this.authService.isLoggedIn();
  }

  toggleDropdown() {
  this.isDropdownOpen = !this.isDropdownOpen;
  }

  //Đóng dropdown khi nhấn ra ngoài 
  @HostListener('document:click', ['$event'])
  handleClickOutside(event: Event) {
    const target = event.target as HTMLElement;
    const clickedInside = target.closest('.user-profile');
    if (!clickedInside) {
      this.isDropdownOpen = false;
    }
  }
}