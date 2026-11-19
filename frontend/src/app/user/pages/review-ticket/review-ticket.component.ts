import { Component, OnInit, ViewChild, HostListener} from '@angular/core';
import { Router } from '@angular/router';
import { ActivatedRoute } from '@angular/router';
import { CommonModule} from '@angular/common';
import { EventsService } from '../../services/events.service';
import { UserService } from '../../services/user.service';
import { ToastNotificationComponent } from '../../pop-up/toast-notification/toast-notification.component';
import { FormsModule } from '@angular/forms';
import { isPlatformBrowser } from '@angular/common';
import { Inject, PLATFORM_ID } from '@angular/core';
import { error } from 'console';

@Component({
  selector: 'app-review-ticket',
  imports: [CommonModule, ToastNotificationComponent, FormsModule],
  templateUrl: './review-ticket.component.html',
  styleUrl: './review-ticket.component.css'
})
export class ReviewTicketComponent implements OnInit {
  eventId!: number;
  eventData: any = {};
  reviews: any[] = [];
  selectedRating = 0;
  comment = '';
  activeMenuId: number | null = null;
  currentUserId: number | null = null;
  isEditing = false;
  editingReviewId: number | null = null;
  showDeletePopup = false;
  reviewToDelete: any = null;
  hasMyReview = false;

  @ViewChild('notification') notification!: ToastNotificationComponent;
  
  constructor(private router: Router,private route: ActivatedRoute,
    private eventsService: EventsService,
    private userService: UserService,
   @Inject(PLATFORM_ID) private platformId: Object) { }

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      this.eventId = +params['id'];
      this.loadEventDetail(this.eventId);
    });

    if (isPlatformBrowser(this.platformId)) {
      const storedId = localStorage.getItem('userId');
      this.currentUserId = storedId ? Number(storedId) : null;
    }
    this.loadReviews(this.eventId);
  }

  toggleMenu(reviewId: number, event: MouseEvent) {
    event.stopPropagation(); // chặn click lan ra ngoài
    this.activeMenuId = this.activeMenuId === reviewId ? null : reviewId;
  }

  // Lắng nghe click ở bất kỳ đâu
  @HostListener('document:click')
  closeMenu() {
    this.activeMenuId = null;
  }

  loadEventDetail(id: number): void {
    this.eventsService.getEventById(id).subscribe({
      next: (res) => {
        const event = res?.data;
        this.eventData = {
          id: event.eventId,
          eventName: event.eventName,
          description: event.description,
          startTime: event.startTime,
          ticketsSaleStartTime: event.ticketsSaleStartTime,
          backgroundUrl: event.backgroundUrl,
          organizerName: event.organizerName,
          organizerBio: event.organizerBio,
          organizerAvatarUrl: event.organizerAvatarUrl,
          address: event.address
        };
      },
      error: (err) => {
        console.error('Lỗi khi lấy chi tiết sự kiện:', err);
      }
    });
  }

  loadReviews(id: number) {
    this.userService.getReviewsByEvent(id).subscribe({
      next: (res) => {
        let currentUserId = this.currentUserId

        const allReviews = (res?.data.reviewDetails || []) as {
          reviewId: number;
          userId: number;
          userFullName: string;
          userProfilePicture: string | null;
          rating: number;
          comment: string;
          reviewDate: string;
        }[];

        const myReview = allReviews.find(r => r.userId === currentUserId);
        const otherReviews = allReviews.filter(r => r.userId !== currentUserId);

        this.reviews = myReview ? [myReview, ...otherReviews] : otherReviews;

        if (myReview) {
          this.hasMyReview = false;
          this.isEditing = true;
          this.editingReviewId = myReview.reviewId;
          this.selectedRating = myReview.rating;
          this.comment = myReview.comment;
        } else {
          this.hasMyReview = true;
        }
      },
      error: (err) => {
        console.error('Lỗi khi lấy review:', err);
      }
    });
  }

  selectRating(rating: number) {
    this.selectedRating = rating;
  }


  deleteReview(review: any) {
    if (review.userId !== this.currentUserId) {
      this.notification.showNotification('Bạn không có quyền xóa đánh giá của người khác',
        3000,
        "warning"
      );
      return;
    }
    this.reviewToDelete = review;
    this.showDeletePopup = true;
  }

  cancelDelete() {
    this.showDeletePopup = false;
    this.reviewToDelete = null;
  }

  confirmDelete() {
    if (!this.reviewToDelete) return;

    this.userService.deleteReview(this.reviewToDelete.reviewId).subscribe({
      next: () => {
        this.notification.showNotification('Xóa đánh giá thành công!',
          3000,
          "warning"
        );
        this.loadReviews(this.eventId);
        this.showDeletePopup = false;
        this.reviewToDelete = null;
        this.isEditing = false;
        this.selectedRating = 0;
        this.comment = '';
      },
      error: (err) => {
        console.error('Lỗi khi xóa đánh giá:', err);
      }
    });
  }

  submitReview() {
    if (this.selectedRating === 0 || !this.comment.trim()) {
      this.notification.showNotification('Vui lòng chọn số sao và nhập nội dung!',
        3000,
        "warning"
      );
      return;
    }

    const reviewData = {
      rating: this.selectedRating,
      comment: this.comment.trim()
    };

    if (this.isEditing && this.editingReviewId) {
      this.userService.updateReview(this.editingReviewId, reviewData).subscribe({
        next: () => {
          this.notification.showNotification('Cập nhật đánh giá thành công!',
            3000,
            "success"
          );
          this.isEditing = false;
          this.editingReviewId = null;
          this.selectedRating = 0;
          this.comment = '';
          this.loadReviews(this.eventId);
        },
        error: (err) => {
          console.error('Lỗi khi cập nhật đánh giá:', err);
        }
      });
    } else {
      this.userService.uploadReview(this.eventId, reviewData).subscribe({
        next: () => {
          this.notification.showNotification('Gửi đánh giá thành công!',
            3000,
            "success");
          this.selectedRating = 0;
          this.comment = '';
          this.loadReviews(this.eventId);
        },
        error: (err) => {
          console.error('Lỗi khi gửi đánh giá', err);
        }
      });
    }
  }

  onNotificationClose() {}
}
