import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PaymentTicketsComponent } from './payment-tickets.component';

describe('PaymentTicketsComponent', () => {
  let component: PaymentTicketsComponent;
  let fixture: ComponentFixture<PaymentTicketsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PaymentTicketsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PaymentTicketsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
