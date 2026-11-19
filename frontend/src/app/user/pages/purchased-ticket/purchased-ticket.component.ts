import { Component, OnInit } from '@angular/core';
import { NavUserComponent } from '../../../component/nav-user/nav-user.component';
import { CommonModule } from '@angular/common';
import { AllComponent } from '../tabs/all/all.component';
import { CancelComponent } from '../tabs/cancel/cancel.component';
import { ProcessingComponent } from '../tabs/processing/processing.component';
import { SuccessComponent } from '../tabs/success/success.component';
import { Router} from '@angular/router';

@Component({
  selector: 'app-purchased-ticket',
  imports: [ NavUserComponent, CommonModule, AllComponent, CancelComponent, ProcessingComponent, SuccessComponent ],
  templateUrl: './purchased-ticket.component.html',
  styleUrl: './purchased-ticket.component.css'
})

export class PurchasedTicketComponent implements OnInit {
  selectedTab: string = 'all';

  setTab(tab: string) {
    this.selectedTab = tab;
    this.onTabChange(tab);
  }

  constructor(private router: Router) { }

  ngOnInit() {}

  private onTabChange(tab: string) {
    switch(tab) {
      case 'all':
        break;
      case 'success':
        break;
      case 'processing':
        break;
      case 'cancel':
        break;
      default:
        console.log('Unknown tab:', tab);
    }
  }

  // Method để check xem tab có đang active không
  isTabActive(tab: string): boolean {
    return this.selectedTab === tab;
  }

  goToHome() {
    this.router.navigate(['/']);
  }
}
