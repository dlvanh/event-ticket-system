import { Routes } from '@angular/router';
import { RoleGuard } from './auth/services/role.guard';
import { AuthGuardService } from './auth/services/auth-guard.service';
import { PendingChangesGuard } from './auth/services/pending-changes.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./auth/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'register',
    loadComponent: () => import('./auth/register/register.component').then(m => m.RegisterComponent)
  },
  {
    path: 'forgot-password',
    loadComponent: () => import('./auth/forgot-password/forgot-password.component').then(m => m.ForgotPasswordComponent)
  },
  {
    path: 'register-organizer',
    loadComponent: () => import('./auth/register-organizer/register-organizer.component').then(m => m.RegisterOrganizerComponent)
  },
  {
    path: 'organizer',
    loadComponent: () => import('./component/nav-organizer/nav-organizer.component').then(m => m.NavOrganizerComponent),
    canActivate: [RoleGuard],
    data: { expectedRole: 'ROLE_organizer' },
    children: [
      { path: '', redirectTo: 'events', pathMatch: 'full' },
      {
        path: 'events',
        loadComponent: () => import('./organizer/pages/home-organizer/home-organizer.component').then(m => m.HomeOrganizerComponent)
      },
      {
        path: 'export-file',
        loadComponent: () => import('./organizer/pages/export-file/export-file.component').then(m => m.ExportFileComponent)
      },
      {
        path: 'create-event',
        loadComponent: () => import('./organizer/pages/create-event/create-event.component').then(m => m.CreateEventComponent)
      },
      {
        path: 'organizer-profile',
        loadComponent: () => import('./organizer/pages/profile/profile.component').then(m => m.ProfileComponent)
      },
      {
        path: 'review-event',
        loadComponent: () => import('./organizer/pages/reviews-event/reviews-event.component').then(m => m.ReviewsEventComponent)
      },
      {
        path: 'export-file-detail/:id',
        loadComponent: () => import('./organizer/pages/export-file-detail/export-file-detail.component').then(m => m.ExportFileDetailComponent)
      }
    ]
  },
  {
    path: 'home',
    loadComponent: () => import('./user/pages/home-user/home-user.component').then(m => m.HomeUserComponent)
  },
  {
    path: 'detail-ticket/:id',
    loadComponent: () => import('./user/pages/detail-ticket/detail-ticket.component').then(m => m.DetailTicketComponent)
  },
  {
    path: 'select-ticket/:id',
    loadComponent: () => import('./user/pages/select-ticket/select-ticket.component').then(m => m.SelectTicketComponent),
    canActivate: [AuthGuardService]
  },
  {
    path: 'user-profile',
    loadComponent: () => import('./user/pages/user-profile/user-profile.component').then(m => m.UserProfileComponent),
    canActivate: [AuthGuardService]
  },
  {
    path: 'purchased-ticket',
    loadComponent: () => import('./user/pages/purchased-ticket/purchased-ticket.component').then(m => m.PurchasedTicketComponent),
    canActivate: [AuthGuardService],
    children: [
      {
        path: 'all',
        loadComponent: () => import('./user/pages/tabs/all/all.component').then(m => m.AllComponent)
      },
      {
        path: 'success',
        loadComponent: () => import('./user/pages/tabs/success/success.component').then(m => m.SuccessComponent)
      },
      {
        path: 'processing',
        loadComponent: () => import('./user/pages/tabs/processing/processing.component').then(m => m.ProcessingComponent)
      },
      {
        path: 'cancel',
        loadComponent: () => import('./user/pages/tabs/cancel/cancel.component').then(m => m.CancelComponent)
      },
      { path: '', redirectTo: 'all', pathMatch: 'full' }
    ]
  },
  {
    path: 'order-ticket',
    loadComponent: () => import('./user/pages/order-ticket/order-ticket.component').then(m => m.OrderTicketComponent),
    canActivate: [AuthGuardService],
    canDeactivate: [PendingChangesGuard]
  },
  {
    path: 'search-events',
    loadComponent: () => import('./user/pages/search-events/search-events.component').then(m => m.SearchEventsComponent)
  },
  {
    path: 'slider-captcha',
    loadComponent: () => import('./user/pages/slider-captcha/slider-captcha.component').then(m => m.SliderCaptchaComponent)
  },
  {
    path: 'review-ticket/:id',
    loadComponent: () => import('./user/pages/review-ticket/review-ticket.component').then(m => m.ReviewTicketComponent),
    canActivate: [AuthGuardService]
  },
  {
    path: 'admin',
    loadComponent: () => import('./admin/pages/home-admin/home-admin.component').then(m => m.HomeAdminComponent),
    canActivate: [RoleGuard],
    data: { expectedRole: 'ROLE_admin' },
    children: [
      {
        path: '',
        loadComponent: () => import('./admin/pages/dashboard/dashboard.component').then(m => m.DashboardComponent)
      },
      {
        path: 'approval-request',
        loadComponent: () => import('./admin/pages/approval-request/approval-request.component').then(m => m.ApprovalRequestComponent)
      },
      {
        path: 'user-management',
        loadComponent: () => import('./admin/pages/user-management/user-management.component').then(m => m.UserManagementComponent)
      },
      {
        path: 'orders-request',
        loadComponent: () => import('./admin/pages/orders-request/orders-request.component').then(m => m.OrdersRequestComponent)
      }
    ]
  },
  { path: '', redirectTo: '/home', pathMatch: 'full' },
  { path: '**', redirectTo: '/home' }
];
