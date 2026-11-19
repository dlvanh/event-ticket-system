import { Component, ViewChild, ElementRef, OnInit, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, FormBuilder, FormGroup, FormArray, Validators, ReactiveFormsModule } from '@angular/forms';
import { ImageCropperModule } from 'ngx-image-cropper';
import { CreateEventsService } from '../../services/create-events.service';
import { LocationService, Location as LocationData } from '../../services/location.service';
import { HttpClientModule } from '@angular/common/http';
import { ToastNotificationComponent } from '../../../user/pop-up/toast-notification/toast-notification.component';
import { Router } from '@angular/router';
import { timeout, catchError, of } from 'rxjs';

interface EventForm {
  eventName: string;
  venueName: string;
  province: number | '';
  district: number | '';
  ward: number | '';
  streetAddress: string;
  category: string;
  description: string;
  organizationName: string;
  organizationInfo: string;
  organizationDescription: string;
}

@Component({
  selector: 'app-create-event',
  standalone: true,
  imports: [CommonModule, FormsModule, ImageCropperModule, ReactiveFormsModule, HttpClientModule, ToastNotificationComponent],
  templateUrl: './create-event.component.html',
  styleUrls: ['./create-event.component.css']
})
export class CreateEventComponent implements OnInit {
  @ViewChild('eventLogoInput') eventLogoInput!: ElementRef<HTMLInputElement>;
  @ViewChild('eventBackgroundInput') eventBackgroundInput!: ElementRef<HTMLInputElement>;
  @ViewChild('organizationLogoInput') organizationLogoInput!: ElementRef<HTMLInputElement>;
  @ViewChild('toastNotification') toastNotification!: ToastNotificationComponent;

  eventForm: FormGroup;
  ticketForm: FormGroup;

  eventLogoPreview: string | null = null;
  eventBackgroundPreview: string | null = null;
  organizationLogoPreview: string | null = null;

  eventLogoFile: File | null = null;
  eventBackgroundFile: File | null = null;
  organizationLogoFile: File | null = null;
  private selectedFileForCropping: File | null = null;

  imageChangedEvent: any = '';
  croppedImage: string = '';
  croppingType: 'eventLogo' | 'eventBackground' | 'organizationLogo' | null = null;

  charCounts: { [key: string]: number } = {
    eventName: 0,
    venueName: 0,
    organizationName: 0,
    organizationInfo: 0,
    organizationDescription: 0
  };

  provinces: LocationData[] = [];
  districts: LocationData[] = [];
  wards: LocationData[] = [];

  defaultEventDescription = 
  `Thông tin sự kiện (mẫu) [Trình bày ngắn gọn sự kiện 10% Nội dung chính...]
  CÁC LƯU Ý KHI TẠO SỰ KIỆN:
  - Khán giả: [Thông tin khách mời...]
  - Thời gian/dự kiến diễn ra sự kiện: [Thời gian, quà tặng...]

  ĐỊA ĐIỂM DIỄN RA SỰ KIỆN:
  [Địa chỉ cụ thể]
  Lưu ý: dự kiến khách mời
  Lưu ý: dự kiến khách VVIP`;

  @Output() formChange = new EventEmitter<any>();

  // Toast notification properties
  showToast: boolean = false;
  toastMessage: string = '';
  toastType: 'success' | 'error' | 'warning' = 'error';
  
  // Confirmation dialog properties
  showConfirmDialog: boolean = false;

  // Loading state
  isSubmitting: boolean = false;

  constructor(
    private fb: FormBuilder,
    private createEventService: CreateEventsService,
    private locationService: LocationService,
    private router: Router
  ) {
    this.eventForm = this.fb.group({
      eventName: ['', Validators.required],
      venueName: ['', Validators.required],
      province: ['', Validators.required],
      district: [''],
      ward: [''],
      streetAddress: [''],
      category: ['', Validators.required],
      description: [this.defaultEventDescription],
      organizationName: [''],
      organizationInfo: [''],
      organizationDescription: ['']
    });

    this.ticketForm = this.fb.group({
      startDate: ['', Validators.required],
      startTime: ['', Validators.required],
      endDate: ['', Validators.required],
      endTime: ['', Validators.required],
      eventDate: ['', Validators.required],
      eventTime: ['', Validators.required],
      eventEndDate: ['', Validators.required],
      eventEndTime: ['', Validators.required],
      tickets: this.fb.array([this.createTicketGroup()]),
      discounts: this.fb.array([this.createDiscount()])
    });
  }

  ngOnInit(): void {
    this.loadProvinces();
  }

  loadProvinces(): void {
    this.locationService.getProvinces().subscribe({
      next: (data) => {
        this.provinces = data;
      },
      error: (err) => {
        console.error('Lỗi khi tải tỉnh/thành phố:', err);
        this.showErrorToast('Không thể tải danh sách tỉnh/thành phố. Vui lòng thử lại sau.');
      }
    });
  }

  triggerFileInput(inputType: string): void {
    switch (inputType) {
      case 'eventLogo':
        this.croppingType = 'eventLogo';
        this.eventLogoInput.nativeElement.click();
        break;
      case 'eventBackground':
        this.croppingType = 'eventBackground';
        this.eventBackgroundInput.nativeElement.click();
        break;
      case 'organizationLogo':
        this.croppingType = 'organizationLogo';
        this.organizationLogoInput.nativeElement.click();
        break;
    }
  }

  onImageSelected(event: Event, imageType: string): void {
    const validTypes = ['eventLogo', 'eventBackground', 'organizationLogo'];
    if (!validTypes.includes(imageType)) {
      console.error('Invalid image type:', imageType);
      return;
    }

    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;

    const file = input.files[0];
    if (!file.type.startsWith('image/')) {
      this.showErrorToast('Vui lòng chọn file hình ảnh!');
      return;
    }

    // Kiểm tra kích thước file (5MB)
    const maxSize = 5 * 1024 * 1024; // 5MB
    if (file.size > maxSize) {
      this.showErrorToast('Kích thước file không được vượt quá 5MB!');
      return;
    }

    // Kiểm tra định dạng file
    const allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp'];
    if (!allowedTypes.includes(file.type)) {
      this.showErrorToast('Chỉ chấp nhận file JPG, PNG hoặc WebP!');
      return;
    }

    this.selectedFileForCropping = file;
    this.imageChangedEvent = event;
    this.croppingType = imageType as 'eventLogo' | 'eventBackground' | 'organizationLogo';
    this.emitFormChange();
  }

  imageCropped(event: any) {
    this.croppedImage = event.base64!;
  }

  saveCroppedImage(): void {
    if (!this.croppingType || !this.croppedImage || !this.selectedFileForCropping) {
      this.cancelCrop();
      return;
    }

    const imageFile = this.base64ToFile(this.croppedImage, this.selectedFileForCropping.name);

    switch (this.croppingType) {
      case 'eventLogo':
        this.eventLogoPreview = this.croppedImage;
        this.eventLogoFile = imageFile;
        break;
      case 'eventBackground':
        this.eventBackgroundPreview = this.croppedImage;
        this.eventBackgroundFile = imageFile;
        break;
      case 'organizationLogo':
        this.organizationLogoPreview = this.croppedImage;
        this.organizationLogoFile = imageFile;
        break;
    }
    this.cancelCrop();
    this.emitFormChange();
  }

  cancelCrop(): void {
    this.imageChangedEvent = '';
    this.croppedImage = '';
    this.croppingType = null;
    this.selectedFileForCropping = null;
  }

  updateCharCount(field: string, event: Event): void {
    const input = event.target as HTMLInputElement | HTMLTextAreaElement;
    this.charCounts[field] = input.value.length;
    this.emitFormChange();
  }

  getCharCount(field: string): number {
    return this.charCounts[field] || 0;
  }

  onProvinceChange(): void {
    const provinceId = this.eventForm.get('province')?.value;
    this.districts = [];
    this.wards = [];
    this.eventForm.patchValue({ district: '', ward: '' });

    if (provinceId) {
      this.locationService.getDistricts(provinceId).subscribe({
        next: (data) => {
          this.districts = data;
        },
        error: (err) => {
          console.error('Lỗi khi tải quận/huyện:', err);
          this.showErrorToast('Không thể tải danh sách quận/huyện. Vui lòng thử lại sau.');
        }
      });
    }
    this.emitFormChange();
  }

  onDistrictChange(): void {
    const districtId = this.eventForm.get('district')?.value;
    this.wards = [];
    this.eventForm.patchValue({ ward: '' });

    if (districtId) {
      this.locationService.getWards(districtId).subscribe({
        next: (data) => {
          this.wards = data;
        },
        error: (err) => {
          console.error('Lỗi khi tải phường/xã:', err);
          this.showErrorToast('Không thể tải danh sách phường/xã. Vui lòng thử lại sau.');
        }
      });
    }
    this.emitFormChange();
  }

  onSubmit(): void {
    if (!this.eventForm.valid) {
      this.showErrorToast('Vui lòng điền đầy đủ thông tin sự kiện!');
      this.markFormGroupTouched();
      return;
    }

    if (!this.ticketForm.valid || !this.isEndTimeAfterStart || !this.isEventDateValid || !this.isEventEndTimeAfterStart) {
      this.showErrorToast('Vui lòng kiểm tra thông tin vé!');
      this.markFormGroupTouched();
      return;
    }

    if (!this.eventLogoFile || !this.eventBackgroundFile) {
      this.showErrorToast('Vui lòng cung cấp logo và ảnh nền cho sự kiện!');
      return;
    }

    // Set loading state
    this.isSubmitting = true;

    const eventData = {
      eventName: this.eventForm.value.eventName,
      description: this.eventForm.value.description,
      provinces: this.eventForm.value.province,
      districts: this.eventForm.value.district,
      wardId: this.eventForm.value.ward,
      addressDetail: this.eventForm.value.streetAddress,
      addressName: this.eventForm.value.venueName,
      category: this.eventForm.value.category,
      organizationName: this.eventForm.value.organizationName,
      organizationInfo: this.eventForm.value.organizationInfo,
      organizationDescription: this.eventForm.value.organizationDescription,
    };

    const ticketData = {
      startTime: `${this.ticketForm.value.eventDate}T${this.ticketForm.value.eventTime}:00`,
      endTime: `${this.ticketForm.value.eventEndDate}T${this.ticketForm.value.eventEndTime}:00`,
      saleStart: `${this.ticketForm.value.startDate}T${this.ticketForm.value.startTime}:00`,
      saleEnd: `${this.ticketForm.value.endDate}T${this.ticketForm.value.endTime}:00`,
      tickets: this.ticketForm.value.tickets.map((ticket: any) => ({
        ticketType: ticket.name,
        quantityTotal: ticket.quantity,
        price: ticket.price
      })),
      discounts: this.ticketForm.value.discounts
    };

    const payload = { ...eventData, ...ticketData };

    this.createEventService.createEvent(payload, this.eventLogoFile, this.eventBackgroundFile)
      .pipe(
        timeout(50000), // Set a timeout of 30 seconds for file upload
        catchError(err => {
          console.error('Request error:', err);
          this.isSubmitting = false;
          
          if (err.name === 'TimeoutError') {
            this.showErrorToast('Yêu cầu tạo sự kiện bị timeout. Vui lòng thử lại.');
          } else {
            this.showErrorToast('Đã có lỗi xảy ra khi tạo sự kiện. Vui lòng thử lại.');
          }
          return of(null);
        })
      )
      .subscribe({
        next: (response) => {
          if (response) {
            console.log('Sự kiện đã được tạo thành công:', response);
            this.isSubmitting = false;
            this.showSuccessToast('Sự kiện đã được tạo thành công!');
            
            // Điều hướng về trang sự kiện sau 2 giây
            setTimeout(() => {
              this.router.navigate(['/organizer/events']);
            }, 2000);
          }
        },
        error: (error) => {
          console.error('Lỗi khi tạo sự kiện:', error);
          this.isSubmitting = false;
          
          let errorMessage = 'Đã có lỗi xảy ra khi tạo sự kiện. Vui lòng thử lại.';
          if (error.status === 400) {
            errorMessage = 'Dữ liệu không hợp lệ. Vui lòng kiểm tra lại thông tin.';
          } else if (error.status === 500) {
            errorMessage = 'Lỗi server. Vui lòng thử lại sau.';
          } else if (error.status === 0) {
            errorMessage = 'Không thể kết nối đến server. Vui lòng kiểm tra kết nối mạng.';
          } else if (error.status === 413) {
            errorMessage = 'File ảnh quá lớn. Vui lòng chọn ảnh nhỏ hơn.';
          }
          this.showErrorToast(errorMessage);
        }
      });

    this.emitFormChange();
  }

  private validateForm(): boolean {
    if (!this.eventForm.valid) {
      this.markFormGroupTouched();
      return false;
    }
    return true;
  }

  onCancel(): void {
    this.showConfirmDialog = true;
  }

  confirmCancel(): void {
    this.resetForm();
    this.showConfirmDialog = false;
  }

  cancelConfirmDialog(): void {
    this.showConfirmDialog = false;
  }

  private resetForm(): void {
    this.eventForm.reset({
      eventName: '',
      venueName: '',
      province: '',
      district: '',
      ward: '',
      streetAddress: '',
      category: '',
      description: this.defaultEventDescription,
      organizationName: '',
      organizationInfo: '',
      organizationDescription: ''
    });

    this.ticketForm.reset();
    this.tickets.clear();
    this.tickets.push(this.createTicketGroup());

    this.eventLogoPreview = null;
    this.eventBackgroundPreview = null;
    this.organizationLogoPreview = null;
    this.charCounts = {
      eventName: 0,
      venueName: 0,
      organizationName: 0,
      organizationInfo: 0,
      organizationDescription: 0
    };
    this.eventLogoFile = null;
    this.eventBackgroundFile = null;
    this.organizationLogoFile = null;
  }

  private base64ToFile(base64: string, filename: string): File {
    const arr = base64.split(',');
    if (arr.length < 2) {
      throw new Error('Invalid base64 string');
    }
    const mimeMatch = arr[0].match(/:(.*?);/);
    if (!mimeMatch || mimeMatch.length < 2) {
      throw new Error('Could not determine mime type from base64 string');
    }
    const mime = mimeMatch[1];
    const bstr = atob(arr[1]);
    let n = bstr.length;
    const u8arr = new Uint8Array(n);
    while (n--) {
      u8arr[n] = bstr.charCodeAt(n);
    }
    return new File([u8arr], filename, { type: mime });
  }

  createTicketGroup(): FormGroup {
    return this.fb.group({
      name: ['', Validators.required],
      price: [1, [Validators.required, Validators.min(1)]],
      quantity: [1, [Validators.required, Validators.min(1)]]
    });
  }

  get tickets(): FormArray {
    return this.ticketForm.get('tickets') as FormArray;
  }

  get isEndTimeAfterStart(): boolean {
    const formValue = this.ticketForm.value;
    if (!formValue.startDate || !formValue.startTime || !formValue.endDate || !formValue.endTime) return true;

    const start = new Date(`${formValue.startDate}T${formValue.startTime}`);
    const end = new Date(`${formValue.endDate}T${formValue.endTime}`);
    return end > start;
  }

  get isEventEndTimeAfterStart(): boolean {
    const formValue = this.ticketForm.value;
    if (!formValue.eventDate || !formValue.eventTime || !formValue.eventEndDate || !formValue.eventEndTime) {
      return true;
    }
    const start = new Date(`${formValue.eventDate}T${formValue.eventTime}`);
    const end = new Date(`${formValue.eventEndDate}T${formValue.eventEndTime}`);
    return end > start;
  }

  get isEventDateValid(): boolean {
    const formValue = this.ticketForm.value;
    if (!formValue.startDate || !formValue.eventDate) return true;

    const start = new Date(formValue.startDate);
    const event = new Date(formValue.eventDate);
    return event >= start;
  }

  get isFormValid(): boolean {
    return this.eventForm.valid && this.ticketForm.valid && this.isEndTimeAfterStart && this.isEventDateValid && this.isEventEndTimeAfterStart;
  }

  addTicket(): void {
    this.tickets.push(this.createTicketGroup());
    this.emitFormChange();
  }

  removeTicket(index: number): void {
    if (this.tickets.length > 1) {
      this.tickets.removeAt(index);
      this.emitFormChange();
    }
  }

  private markFormGroupTouched(): void {
    Object.values(this.eventForm.controls).forEach(control => {
      if (control instanceof FormGroup || control instanceof FormArray) {
        (control as any).markAllAsTouched();
      } else {
        control.markAsTouched();
      }
    });

    Object.values(this.ticketForm.controls).forEach(control => {
      if (control instanceof FormGroup || control instanceof FormArray) {
        (control as any).markAllAsTouched();
      } else {
        control.markAsTouched();
      }
    });
  }

  emitFormChange() {
    this.formChange.emit({
      eventForm: this.eventForm.value,
      ticketForm: this.ticketForm.value,
      eventLogoFile: this.eventLogoFile,
      eventBackgroundFile: this.eventBackgroundFile,
      organizationLogoFile: this.organizationLogoFile
    });
  }

  formatPrice(index: number) {
    const control = this.tickets.at(index).get('price');
    if (!control) return;
    
    let value = control.value?.toString().replace(/\D/g, '');
    if (!value) {
      control.setValue(null, { emitEvent: false });
      return;
    }
  
    const numberValue = Number(value);
    if (isNaN(numberValue) || numberValue < 0) {
      control.setValue(null, { emitEvent: false });
      return;
    }
    
    const displayValue = numberValue.toLocaleString('vi-VN');
  
    // Set hiển thị
    const inputElement = document.getElementById(`ticketPrice-${index}`) as HTMLInputElement;
    if (inputElement) {
      inputElement.value = displayValue;
    }
  
    // Dữ liệu thật
    control.setValue(numberValue, { emitEvent: false });
  }
  
  createDiscount(): FormGroup {
  return this.fb.group({
    discountCode: ['', Validators.required],
    discountDescription: ['', Validators.required],
    discountType: ['percentage', Validators.required],
    discountValue: [null, [Validators.required, Validators.min(1)]],
    discountValidFrom: ['', Validators.required],
    discountValidTo: ['', Validators.required],
    discountMaxUses: [1, [Validators.required, Validators.min(1)]],
  });
  }

  get discounts(): FormArray {
    return this.ticketForm.get('discounts') as FormArray;
  }

  addDiscount() {
    this.discounts.push(this.createDiscount());
  }

  removeDiscount(index: number) {
    this.discounts.removeAt(index);
  }

  // Toast notification methods
  showSuccessToast(message: string): void {
    this.toastType = 'success';
    this.toastMessage = message;
    this.showToast = true;
    this.toastNotification.showNotification(message, 5000, 'success');
  }

  showErrorToast(message: string): void {
    this.toastType = 'error';
    this.toastMessage = message;
    this.showToast = true;
    this.toastNotification.showNotification(message, 5000, 'error');
  }

  showWarningToast(message: string): void {
    this.toastType = 'warning';
    this.toastMessage = message;
    this.showToast = true;
    this.toastNotification.showNotification(message, 5000, 'warning');
  }
}