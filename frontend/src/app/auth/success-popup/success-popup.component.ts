import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

@Component({
    selector: 'app-success-popup',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './success-popup.component.html',
    styleUrl: './success-popup.component.css',
})
export class SuccessPopupComponent {
    @Input() redirectTo: string = '/home';
    @Input() message: string = 'Đặt lại mật khẩu thành công!';
    @Input() delay: number = 5;
    @Output() closed = new EventEmitter<void>();

    countdown = this.delay;
    intervalId: any;

    constructor(private router: Router) { }

    ngOnInit() {
        this.intervalId = setInterval(() => {
            this.countdown--;
            if (this.countdown === 0) {
                clearInterval(this.intervalId);
                this.goNow();
            }
        }, 1000);
    }

    goNow() {
        if (this.intervalId) {
            clearInterval(this.intervalId);
        }
        this.router.navigateByUrl(this.redirectTo);
        this.closed.emit();
    }

    ngOnDestroy() {
        if (this.intervalId) {
            clearInterval(this.intervalId);
        }
    }
}