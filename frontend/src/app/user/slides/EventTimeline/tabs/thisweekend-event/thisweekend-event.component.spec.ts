import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ThisweekendEventComponent } from './thisweekend-event.component';

describe('ThisweekendEventComponent', () => {
  let component: ThisweekendEventComponent;
  let fixture: ComponentFixture<ThisweekendEventComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ThisweekendEventComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ThisweekendEventComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
