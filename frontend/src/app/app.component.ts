import { Component, OnInit  } from '@angular/core';
import { Router, NavigationStart, NavigationEnd, NavigationCancel, NavigationError, Event as NavigationEvent } from '@angular/router';
import { CommonModule } from "@angular/common";
import { RouterOutlet } from '@angular/router';
import { HeaderUserComponent } from "./component/header-user/header-user.component";
import { FooterUserComponent } from "./component/footer-user/footer-user.component";
import { AuthService } from './auth/services/auth.service';
import { LoadingComponent } from './user/pop-up/loading/loading.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    HeaderUserComponent,
    FooterUserComponent
  ],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit {
  title = 'Online_ticketing_system_FE';
  isLoading = true;
  isAuthPage = false;

  private authRoutes = [
    '/login', '/register', '/forgot-password',
    '/organizer', '/admin', '/slider-captcha'
  ];

  constructor(private router: Router) {}

  ngOnInit(): void {
    // Lắng nghe sự kiện chuyển route để hiển thị loading và xác định layout
    this.router.events.subscribe(event => {
      if (event instanceof NavigationStart) {
        this.isLoading = true;
      }

      if (
        event instanceof NavigationEnd ||
        event instanceof NavigationCancel ||
        event instanceof NavigationError
      ) {
        this.isLoading = false;

        const currentUrl = event instanceof NavigationEnd ? event.urlAfterRedirects : this.router.url;
        this.isAuthPage = this.authRoutes.some(route => currentUrl.includes(route));
      }
    });
  }
}
