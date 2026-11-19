import { RouterModule, Routes } from '@angular/router';
import { HomeOrganizerComponent } from './organizer/pages/home-organizer/home-organizer.component';
import { NavOrganizerComponent } from './component/nav-organizer/nav-organizer.component';
import { ExportFileComponent } from './organizer/pages/export-file/export-file.component';
import { HomeUserComponent } from './user/pages/home-user/home-user.component';
import { HomeAdminComponent } from './admin/pages/home-admin/home-admin.component';
import { LoginComponent } from './auth/login/login.component';
import { RegisterComponent } from './auth/register/register.component';
import { ForgotPasswordComponent } from './auth/forgot-password/forgot-password.component';
import { CreateEventComponent } from './organizer/pages/create-event/create-event.component';
import { ProfileComponent } from './organizer/pages/profile/profile.component';
import { DetailTicketComponent } from './user/pages/detail-ticket/detail-ticket.component';
import { ApprovalRequestComponent } from './admin/pages/approval-request/approval-request.component';
import { DashboardComponent } from './admin/pages/dashboard/dashboard.component';
import { UserManagementComponent } from './admin/pages/user-management/user-management.component';
import { SelectTicketComponent } from './user/pages/select-ticket/select-ticket.component';
import { NgModule } from '@angular/core';
import { RoleGuard } from './auth/services/role.guard';
import { UserProfileComponent } from './user/pages/user-profile/user-profile.component';
import { PurchasedTicketComponent } from './user/pages/purchased-ticket/purchased-ticket.component';
import { OrderTicketComponent } from './user/pages/order-ticket/order-ticket.component';
import { SearchEventsComponent } from './user/pages/search-events/search-events.component';
import { PaymentTicketsComponent } from './user/pages/payment-tickets/payment-tickets.component';
import { RegisterOrganizerComponent } from './auth/register-organizer/register-organizer.component';
import { ExportFileDetailComponent } from './organizer/pages/export-file-detail/export-file-detail.component';
import { SliderCaptchaComponent } from './user/pages/slider-captcha/slider-captcha.component';
import { OrdersRequestComponent } from './admin/pages/orders-request/orders-request.component';
import { ReviewsEventComponent } from './organizer/pages/reviews-event/reviews-event.component';
import { ReviewTicketComponent } from './user/pages/review-ticket/review-ticket.component';
import { PendingChangesGuard } from './auth/services/pending-changes.guard';
import { AllComponent } from './user/pages/tabs/all/all.component';
import { SuccessComponent } from './user/pages/tabs/success/success.component';
import { ProcessingComponent } from './user/pages/tabs/processing/processing.component';
import { CancelComponent } from './user/pages/tabs/cancel/cancel.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'forgot-password', component: ForgotPasswordComponent },
  { path: 'register-organizer', component: RegisterOrganizerComponent },

  {
    path: 'organizer',
    component: NavOrganizerComponent,
    canActivate: [RoleGuard], data: { expectedRole: 'ROLE_organizer' },
    children: [
      { path: '', redirectTo: 'events', pathMatch: 'full' },
      { path: 'events', component: HomeOrganizerComponent },
      { path: 'export-file', component: ExportFileComponent },
      { path: 'create-event', component: CreateEventComponent },
      { path: 'organizer-profile', component: ProfileComponent },
      { path: 'review-event', component: ReviewsEventComponent },
      { path: 'event-detail/:id', component: HomeOrganizerComponent },
      { path: 'export-file-detail/:id', component: ExportFileDetailComponent },
    ]
  },

  // Home và các route liên quan
  { path: 'home', component: HomeUserComponent },
  { path: 'detail-ticket/:id', component: DetailTicketComponent },
  { path: 'select-ticket/:id', component: SelectTicketComponent },
  { path: 'user-profile', component: UserProfileComponent },
  {
  path: 'purchased-ticket',
  component: PurchasedTicketComponent,
  children: [
    { path: 'all', component: AllComponent },
    { path: 'success', component: SuccessComponent },
    { path: 'processing', component: ProcessingComponent },
    { path: 'cancel', component: CancelComponent },
    { path: '', redirectTo: 'all', pathMatch: 'full' }
  ]
}
,
  { path: 'order-ticket', component: OrderTicketComponent, canDeactivate: [PendingChangesGuard] },
  { path: 'search-events', component: SearchEventsComponent },
  { path: 'payment-tickets', component: PaymentTicketsComponent },
  { path: 'slider-captcha', component: SliderCaptchaComponent },
  { path: 'review-ticket/:id', component: ReviewTicketComponent },

  {
    path: 'admin',
    component: HomeAdminComponent,
    canActivate: [RoleGuard], data: { expectedRole: 'ROLE_admin' },
    children: [
      { path: '', component: DashboardComponent },
      { path: 'approval-request', component: ApprovalRequestComponent },
      { path: 'user-management', component: UserManagementComponent },
      { path: 'orders-request', component: OrdersRequestComponent },
    ],
  },

  // Chuyển hướng mặc định
  { path: '', redirectTo: '/home', pathMatch: 'full' },
  { path: '**', redirectTo: '/home' },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule],
})
export class AppRoutingModule {}
