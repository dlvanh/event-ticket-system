import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ExportFileDetailComponent } from './export-file-detail.component';

describe('ExportFileDetailComponent', () => {
  let component: ExportFileDetailComponent;
  let fixture: ComponentFixture<ExportFileDetailComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ExportFileDetailComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ExportFileDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
