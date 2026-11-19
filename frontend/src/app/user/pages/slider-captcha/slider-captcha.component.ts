import {
  Component,
  OnInit,
  OnDestroy,
  Output,
  EventEmitter
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';

interface CaptchaChallenge {
  type: 'math' | 'slider';
  question: string;
  correctAnswer: number;
}

@Component({
  selector: 'app-slider-captcha',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './slider-captcha.component.html',
  styleUrl: './slider-captcha.component.css'
})
export class SliderCaptchaComponent implements OnInit, OnDestroy {
  @Output() verificationComplete = new EventEmitter<boolean>();

  eventId!: number;
  currentChallenge!: CaptchaChallenge;
  mathAnswer: number | null = null;
  sliderPosition = 0;
  targetPosition = 0;
  isDragging = false;
  isVerifying = false;
  verificationResult: 'success' | 'failed' | null = null;
  attempts = 0;
  maxAttempts = 3;

  private readonly handleMouseMove = (e: MouseEvent) => this.onDrag(e);
  private readonly handleMouseUp = () => this.stopDrag();
  private readonly handleTouchMove = (e: TouchEvent) => this.onDrag(e);
  private readonly handleTouchEnd = () => this.stopDrag();

  constructor(
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      this.eventId = Number(params['eventId']);
    });
    this.initializeChallenge();
    this.setupSliderEvents();
  }

  ngOnDestroy(): void {
    document.removeEventListener('mousemove', this.handleMouseMove);
    document.removeEventListener('mouseup', this.handleMouseUp);
    document.removeEventListener('touchmove', this.handleTouchMove);
    document.removeEventListener('touchend', this.handleTouchEnd);
  }

  private initializeChallenge(): void {
    const useSlider = Math.random() >= 0.5;
    if (useSlider) {
      this.targetPosition = Math.floor(Math.random() * 240) + 60;
      this.currentChallenge = {
        type: 'slider',
        question: 'Kéo thanh trượt để khớp với chấm đỏ',
        correctAnswer: this.targetPosition
      };
    } else {
      const left = Math.floor(Math.random() * 40) + 10;
      const right = Math.floor(Math.random() * 40) + 5;
      this.currentChallenge = {
        type: 'math',
        question: `Giải phép toán: ${left} + ${right} = ?`,
        correctAnswer: left + right
      };
    }

    this.resetState();
  }

  private resetState(): void {
    this.mathAnswer = null;
    this.sliderPosition = 0;
    this.verificationResult = null;
    this.isVerifying = false;
  }

  private setupSliderEvents(): void {
    document.addEventListener('mousemove', this.handleMouseMove);
    document.addEventListener('mouseup', this.handleMouseUp);
    document.addEventListener('touchmove', this.handleTouchMove);
    document.addEventListener('touchend', this.handleTouchEnd);
  }

  startDrag(event: MouseEvent | TouchEvent): void {
    this.isDragging = true;
    event.preventDefault();
  }

  onDrag(event: MouseEvent | TouchEvent): void {
    if (!this.isDragging) return;

    const clientX = event instanceof MouseEvent ? event.clientX : event.touches[0].clientX;
    const container = document.querySelector('.slider-track') as HTMLElement | null;
    if (!container) return;

    const rect = container.getBoundingClientRect();
    const newPosition = clientX - rect.left - 20;
    this.sliderPosition = Math.max(0, Math.min(newPosition, rect.width - 40));
  }

  stopDrag(): void {
    this.isDragging = false;
  }

  canVerify(): boolean {
    if (this.currentChallenge.type === 'math') {
      return this.mathAnswer !== null;
    }
    if (this.currentChallenge.type === 'slider') {
      return this.sliderPosition > 0;
    }
    return false;
  }

  verifyAnswer(): void {
    this.isVerifying = true;
    this.verificationResult = null;

    setTimeout(() => {
      let isCorrect = false;
      if (this.currentChallenge.type === 'math') {
        isCorrect = Number(this.mathAnswer) === this.currentChallenge.correctAnswer;
      } else if (this.currentChallenge.type === 'slider') {
        isCorrect = Math.abs(this.sliderPosition - this.targetPosition) < 20;
      }

      this.isVerifying = false;
      this.verificationResult = isCorrect ? 'success' : 'failed';

      if (isCorrect) {
        setTimeout(() => {
          this.verificationComplete.emit(true);
          this.router.navigate(['/select-ticket', this.eventId]);
        }, 600);
      } else {
        this.attempts++;
        if (this.attempts >= this.maxAttempts) {
          setTimeout(() => {
            this.verificationComplete.emit(false);
          }, 1200);
        } else {
          setTimeout(() => {
            this.refreshChallenge();
          }, 1200);
        }
      }
    }, 500);
  }

  refreshChallenge(): void {
    this.initializeChallenge();
  }
}
