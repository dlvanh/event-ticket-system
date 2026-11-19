import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ThismonthEventComponent } from './thismonth-event.component';

describe('ThismonthEventComponent', () => {
  let component: ThismonthEventComponent;
  let fixture: ComponentFixture<ThismonthEventComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ThismonthEventComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ThismonthEventComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
