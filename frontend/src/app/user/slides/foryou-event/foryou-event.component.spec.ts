import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ForyouEventComponent } from './foryou-event.component';

describe('ForyouEventComponent', () => {
  let component: ForyouEventComponent;
  let fixture: ComponentFixture<ForyouEventComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ForyouEventComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ForyouEventComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
