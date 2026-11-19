import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SportsEventComponent } from './sports-event.component';

describe('SportsEventComponent', () => {
  let component: SportsEventComponent;
  let fixture: ComponentFixture<SportsEventComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SportsEventComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SportsEventComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
