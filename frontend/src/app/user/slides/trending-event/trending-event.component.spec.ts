import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TrendingEventComponent } from './trending-event.component';

describe('TrendingEventComponent', () => {
  let component: TrendingEventComponent;
  let fixture: ComponentFixture<TrendingEventComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TrendingEventComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TrendingEventComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
