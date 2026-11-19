import { ComponentFixture, TestBed } from '@angular/core/testing';

import { StageArtEventComponent } from './stage-art-event.component';

describe('StageArtEventComponent', () => {
  let component: StageArtEventComponent;
  let fixture: ComponentFixture<StageArtEventComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StageArtEventComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(StageArtEventComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
