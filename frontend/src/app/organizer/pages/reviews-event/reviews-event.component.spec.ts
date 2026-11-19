import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ReviewsEventComponent } from './reviews-event.component';

describe('ReviewsEventComponent', () => {
  let component: ReviewsEventComponent;
  let fixture: ComponentFixture<ReviewsEventComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReviewsEventComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ReviewsEventComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
