import { ComponentFixture, TestBed } from '@angular/core/testing';

import { InterestPlaceComponent } from './interest-place.component';

describe('InterestPlaceComponent', () => {
  let component: InterestPlaceComponent;
  let fixture: ComponentFixture<InterestPlaceComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [InterestPlaceComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(InterestPlaceComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
