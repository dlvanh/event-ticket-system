import { TestBed } from '@angular/core/testing';

import { TicketOrderService } from './ticket-order.service';

describe('TicketOrderService', () => {
  let service: TicketOrderService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(TicketOrderService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
