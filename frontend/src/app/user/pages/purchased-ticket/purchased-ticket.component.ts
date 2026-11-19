import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { NavUserComponent } from '../../../component/nav-user/nav-user.component';

@Component({
  selector: 'app-purchased-ticket',
  standalone: true,
  imports: [NavUserComponent, CommonModule, RouterModule],
  templateUrl: './purchased-ticket.component.html',
  styleUrl: './purchased-ticket.component.css'
})
export class PurchasedTicketComponent {
  constructor(private router: Router) {}

  goToHome() {
    this.router.navigate(['/']);
  }
}
