import { ComponentFixture, TestBed } from '@angular/core/testing';

import { OrdersRequestComponent } from './orders-request.component';

describe('OrdersRequestComponent', () => {
  let component: OrdersRequestComponent;
  let fixture: ComponentFixture<OrdersRequestComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OrdersRequestComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(OrdersRequestComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
