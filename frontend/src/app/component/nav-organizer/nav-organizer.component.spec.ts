import { ComponentFixture, TestBed } from '@angular/core/testing';

import { NavOrganizerComponent } from './nav-organizer.component';

describe('NavOrganizerComponent', () => {
  let component: NavOrganizerComponent;
  let fixture: ComponentFixture<NavOrganizerComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NavOrganizerComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(NavOrganizerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
