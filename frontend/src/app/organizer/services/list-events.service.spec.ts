import { TestBed } from '@angular/core/testing';

import { ListEventsService } from './list-events.service';

describe('ListEventsService', () => {
  let service: ListEventsService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ListEventsService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
