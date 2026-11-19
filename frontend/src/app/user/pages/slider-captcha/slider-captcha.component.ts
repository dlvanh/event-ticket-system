import { Component, 
  ElementRef, 
  HostListener, 
  OnInit, 
  ViewChild,
  Output, 
  EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { UserService } from '../../services/user.service';

interface CaptchaChallenge {
  type: 'image' | 'math' | 'slider';
  question: string;
  options?: string[];
  correctAnswer: number | number[];
  images?: string[];
}

@Component({
  selector: 'app-slider-captcha',
  imports: [CommonModule, FormsModule],
  templateUrl: './slider-captcha.component.html',
  styleUrl: './slider-captcha.component.css'
})
export class SliderCaptchaComponent implements OnInit {
  @Output() verificationComplete = new EventEmitter<boolean>();

  eventId!: number;
  currentChallenge!: CaptchaChallenge;
  selectedImages: number[] = [];
  mathAnswer: number | null = null;
  sliderPosition = 0;
  targetPosition = 0;
  isDragging = false;
  isVerifying = false;
  verificationResult: 'success' | 'failed' | null = null;
  attempts = 0;
  maxAttempts = 3;

  constructor(private userService: UserService, private router: Router, private route: ActivatedRoute) {}

  private challenges: CaptchaChallenge[] = [
    {
      type: 'image',
      question: 'Chọn tất cả hình ảnh có chứa xe hơi',
      correctAnswer: [0, 2],
      images: [
        'https://i.pinimg.com/736x/44/dc/70/44dc70c536977d485f25b3bf0af5a94f.jpg',
        'https://i.pinimg.com/736x/7c/74/22/7c74220062bd04456f90792ce2eedaaa.jpg',
        'https://i.pinimg.com/736x/05/37/dc/0537dc90815356d6ebb74522466b8ef8.jpg',
        'https://i.pinimg.com/736x/6f/d1/53/6fd153b1fa998510c6cf0e2f22b4f6d9.jpg',
        'https://i.pinimg.com/736x/84/28/00/842800d01eed5ef025d1491bc3dd1b58.jpg',
        'https://i.pinimg.com/736x/ff/3d/92/ff3d92d78768597eaec5daa18012f07f.jpg'
      ]
    },
    {
      type: 'math',
      question: 'Giải phép toán: 15 + 27 = ?',
      correctAnswer: 42
    },
    {
      type: 'slider',
      question: 'Kéo thanh trượt để khớp với chấm đỏ',
      correctAnswer: 0
    }
  ];

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      this.eventId = params['eventId'];
    });
    this.initializeChallenge();
    this.setupSliderEvents();
  }

  private initializeChallenge() {
    const randomIndex = Math.floor(Math.random() * this.challenges.length);
    this.currentChallenge = { ...this.challenges[randomIndex] };
    
    if (this.currentChallenge.type === 'slider') {
      this.targetPosition = Math.random() * 300 + 50;
      this.currentChallenge.correctAnswer = this.targetPosition;
    }
    
    this.resetState();
  }

  private resetState() {
    this.selectedImages = [];
    this.mathAnswer = null;
    this.sliderPosition = 0;
    this.verificationResult = null;
    this.isVerifying = false;
  }

  private setupSliderEvents() {
    document.addEventListener('mousemove', (e) => this.onDrag(e));
    document.addEventListener('mouseup', () => this.stopDrag());
    document.addEventListener('touchmove', (e) => this.onDrag(e));
    document.addEventListener('touchend', () => this.stopDrag());
  }

  toggleImageSelection(index: number) {
    const selectedIndex = this.selectedImages.indexOf(index);
    if (selectedIndex > -1) {
      this.selectedImages.splice(selectedIndex, 1);
    } else {
      this.selectedImages.push(index);
    }
  }

  startDrag(event: MouseEvent | TouchEvent) {
    this.isDragging = true;
    event.preventDefault();
  }

  onDrag(event: MouseEvent | TouchEvent) {
    if (!this.isDragging) return;
    
    const clientX = event instanceof MouseEvent ? event.clientX : event.touches[0].clientX;
    const container = document.querySelector('.slider-track') as HTMLElement;
    const rect = container.getBoundingClientRect();
    const newPosition = clientX - rect.left - 20; // 20 is half of slider width
    
    this.sliderPosition = Math.max(0, Math.min(newPosition, rect.width - 40));
  }

  stopDrag() {
    this.isDragging = false;
  }

  canVerify(): boolean {
    if (this.currentChallenge.type === 'image') {
      return this.selectedImages.length > 0;
    }
    if (this.currentChallenge.type === 'math') {
      return this.mathAnswer !== null;
    }
    if (this.currentChallenge.type === 'slider') {
      return Math.abs(this.sliderPosition - this.targetPosition) < 20;
    }
    return false;
  }

  verifyAnswer() {
    this.isVerifying = true;
    this.verificationResult = null;
    
    // Simulate verification delay
    setTimeout(() => {
      let isCorrect = false;
      
      if (this.currentChallenge.type === 'image') {
        const correctAnswers = this.currentChallenge.correctAnswer as number[];
        isCorrect = this.selectedImages.length === correctAnswers.length && 
                   this.selectedImages.every(img => correctAnswers.includes(img));
      } else if (this.currentChallenge.type === 'math') {
        isCorrect = this.mathAnswer === this.currentChallenge.correctAnswer;
      } else if (this.currentChallenge.type === 'slider') {
        isCorrect = Math.abs(this.sliderPosition - this.targetPosition) < 20;
      }
      
      this.isVerifying = false;
      this.verificationResult = isCorrect ? 'success' : 'failed';
      
      if (isCorrect) {
        setTimeout(() => {
          this.verificationComplete.emit(true);
          this.router.navigate(['/select-ticket', this.eventId]);
        }, 1500);
      } else {
        this.attempts++;
        if (this.attempts >= this.maxAttempts) {
          setTimeout(() => {
            this.verificationComplete.emit(false);
          }, 2000);
        } else {
          setTimeout(() => {
            this.refreshChallenge();
          }, 2000);
        }
      }
    }, 1500);
  }

  refreshChallenge() {
    this.initializeChallenge();
  }
}

