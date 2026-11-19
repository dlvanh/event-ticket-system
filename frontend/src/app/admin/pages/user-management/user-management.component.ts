
import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { AdminService } from '../service/admin.service';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [CommonModule, HttpClientModule, FormsModule],
  templateUrl: './user-management.component.html',
  styleUrls: ['./user-management.component.css']
})
export class UserManagementComponent implements OnInit {
  users: any[] = [];
  filteredUsers: any[] = [];
  filterRole: string = '';
  filterStatus: string = '';
  isDeleting: boolean = false;
  isDisabling: boolean = false;
  isApproving: boolean = false;
  showSuccessModal: boolean = false;
  showErrorModal: boolean = false;
  showConfirmModal: boolean = false;
  showDetailModal: boolean = false;
  showReviewModal: boolean = false;
  modalMessage: string = '';
  userToDelete: { id: number, name: string } | null = null;
  usersToDelete: number[] = [];
  userToDisable: { id: number, name: string } | null = null;
  usersToDisable: number[] = [];
  userToApprove: { id: number, name: string } | null = null;
  usersToApprove: number[] = [];
  selectedUser: any = null;
  actionType: 'delete' | 'disable' | 'approve' | null = null;
  selectedUsers: number[] = [];
  selectedUserReviews: any[] = [];
  selectedUserFullName: string | null = null;
  isLoading: boolean = true;
  isLoadingReviews: boolean = false;

  constructor(
    private adminService: AdminService,
    private sanitizer: DomSanitizer,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.adminService.getUsers().subscribe({
      next: (response) => {
        console.log('Raw API response:', JSON.stringify(response, null, 2));
        console.log('Dữ liệu người dùng:', response.data.listUsers);
        this.users = response.data.listUsers ?? [];
        this.applyFilter();
        this.selectedUsers = [];
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Lỗi khi lấy danh sách người dùng:', error);
        this.modalMessage = 'Có lỗi xảy ra khi tải danh sách người dùng. Vui lòng thử lại!';
        this.showErrorModal = true;
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  applyFilter(): void {
    this.filteredUsers = this.users.filter(user => {
      const matchRole = !this.filterRole || user.role === this.filterRole;
      const matchStatus = !this.filterStatus || user.status === this.filterStatus;
      return matchRole && matchStatus;
    });
    console.log('Filtered users:', this.filteredUsers);
    this.cdr.detectChanges();
  }

  canApproveSelectedUsers(): boolean {
    return this.selectedUsers.every(userId => {
      const user = this.users.find(u => u.id === userId);
      return user && user.role === 'organizer' && user.status === 'pending';
    });
  }

  logUser(user: any): void {
    console.log('User in ngFor:', user);
    console.log('Role:', user.role, 'Status:', user.status, 'Should show approve button:', user.role === 'organizer' && user.status === 'pending');
  }

  logIsApproving(): void {
    console.log('isApproving:', this.isApproving);
  }

  viewReviews(userId: number, fullName: string | null): void {
    if (!userId) {
      console.error('User ID is undefined');
      this.modalMessage = 'Không thể tải đánh giá: ID người dùng không hợp lệ';
      this.showErrorModal = true;
      return;
    }
    console.log('Calling getReviewsByUserId with userId:', userId);
    this.selectedUserFullName = fullName || 'Unknown';
    this.isLoadingReviews = true;
    this.showReviewModal = true;
    this.adminService.getReviewsByUserId(userId).subscribe({
      next: (response) => {
        console.log('Response from getReviewsByUserId:', JSON.stringify(response, null, 2));
        if (response && response.data && response.data.userReviews && response.data.userReviews.usersReview) {
          this.selectedUserReviews = response.data.userReviews.usersReview.map((review: any) => ({
            reviewId: review.reviewId,
            userId: response.data.userReviews.userId,
            eventId: review.eventSummary.eventId,
            eventName: review.eventSummary.eventName || 'Không xác định',
            category: review.eventSummary.category || 'Không xác định',
            eventStatus: review.eventSummary.status || 'Không xác định',
            startTime: review.eventSummary.startTime ? new Date(review.eventSummary.startTime).toLocaleString('vi-VN') : 'Không xác định',
            endTime: review.eventSummary.endTime ? new Date(review.eventSummary.endTime).toLocaleString('vi-VN') : 'Không xác định',
            eventLogoUrl: review.eventSummary.eventLogoUrl || null,
            rating: review.rating,
            comment: review.comment || 'Không có bình luận',
            reviewDate: review.reviewDate ? new Date(review.reviewDate).toLocaleString('vi-VN') : 'Không xác định'
          }));
          console.log('Processed reviews:', JSON.stringify(this.selectedUserReviews, null, 2));
          this.isLoadingReviews = false;
          this.cdr.detectChanges();
        } else {
          this.selectedUserReviews = [];
          this.modalMessage = 'Không tìm thấy đánh giá nào';
          this.isLoadingReviews = false;
          this.cdr.detectChanges();
        }
      },
      error: (error) => {
        console.error('Error loading reviews:', error);
        const errorMessage = error.error?.message || error.statusText || 'Unknown Error';
        this.modalMessage = `Có lỗi xảy ra khi tải đánh giá: ${errorMessage}. Vui lòng thử lại!`;
        this.showErrorModal = true;
        this.selectedUserReviews = [];
        this.isLoadingReviews = false;
        this.cdr.detectChanges();
      }
    });
  }

  closeReviewModal(): void {
    this.showReviewModal = false;
    this.selectedUserReviews = [];
    this.selectedUserFullName = null;
    this.modalMessage = '';
    if (document.activeElement instanceof HTMLElement) {
      document.activeElement.blur();
    }
    this.cdr.detectChanges();
  }

  isUserSelected(userId: number): boolean {
    return this.selectedUsers.includes(userId);
  }

  toggleUserSelection(userId: number): void {
    if (this.selectedUsers.includes(userId)) {
      this.selectedUsers = this.selectedUsers.filter(id => id !== userId);
    } else {
      this.selectedUsers.push(userId);
    }
    this.cdr.detectChanges();
  }

  toggleSelectAll(event: Event): void {
    const isChecked = (event.target as HTMLInputElement).checked;
    if (isChecked) {
      this.selectedUsers = this.filteredUsers.map(user => user.id);
    } else {
      this.selectedUsers = [];
    }
    this.cdr.detectChanges();
  }

  canDisableSelectedUsers(): boolean {
    return this.selectedUsers.every(userId => {
      const user = this.users.find(u => u.id === userId);
      return user && user.status === 'active';
    });
  }

  showUserDetails(user: any): void {
    console.log('Người dùng được chọn:', user);
    this.selectedUser = user;
    this.showDetailModal = true;
    this.cdr.detectChanges();
  }

  closeDetailModal(): void {
    this.showDetailModal = false;
    this.selectedUser = null;
    this.cdr.detectChanges();
  }

  getSafeImageUrl(url: string | null | undefined): SafeUrl | null {
    if (url) {
      console.log('URL ảnh:', url);
      return this.sanitizer.bypassSecurityTrustUrl(url);
    }
    return null;
  }

  handleImageError(event: Event): void {
    console.log('Lỗi tải ảnh:', (event.target as HTMLImageElement).src);
    (event.target as HTMLImageElement).src = 'assets/default-profile.png';
  }

  confirmDeleteUser(userId: number, userName: string): void {
    this.userToDelete = { id: userId, name: userName };
    this.usersToDelete = [];
    this.userToDisable = null;
    this.usersToDisable = [];
    this.userToApprove = null;
    this.usersToApprove = [];
    this.actionType = 'delete';
    this.showConfirmModal = true;
    this.cdr.detectChanges();
  }

  confirmDeleteMultipleUsers(userIds: number[]): void {
    this.usersToDelete = userIds;
    this.userToDelete = null;
    this.userToDisable = null;
    this.usersToDisable = [];
    this.userToApprove = null;
    this.usersToApprove = [];
    this.actionType = 'delete';
    this.showConfirmModal = true;
    this.cdr.detectChanges();
  }

  confirmDisableUser(userId: number, userName: string): void {
    this.userToDisable = { id: userId, name: userName };
    this.usersToDisable = [];
    this.userToDelete = null;
    this.usersToDelete = [];
    this.userToApprove = null;
    this.usersToApprove = [];
    this.actionType = 'disable';
    this.showConfirmModal = true;
    this.cdr.detectChanges();
  }

  confirmDisableMultipleUsers(userIds: number[]): void {
    this.usersToDisable = userIds;
    this.userToDisable = null;
    this.userToDelete = null;
    this.usersToDelete = [];
    this.userToApprove = null;
    this.usersToApprove = [];
    this.actionType = 'disable';
    this.showConfirmModal = true;
    this.cdr.detectChanges();
  }

  confirmApproveUser(userId: number, userName: string): void {
    this.userToApprove = { id: userId, name: userName };
    this.usersToApprove = [];
    this.userToDelete = null;
    this.usersToDelete = [];
    this.userToDisable = null;
    this.usersToDisable = [];
    this.actionType = 'approve';
    this.showConfirmModal = true;
    this.cdr.detectChanges();
  }

  confirmApproveMultipleUsers(userIds: number[]): void {
    this.usersToApprove = userIds;
    this.userToApprove = null;
    this.userToDelete = null;
    this.usersToDelete = [];
    this.userToDisable = null;
    this.usersToDisable = [];
    this.actionType = 'approve';
    this.showConfirmModal = true;
    this.cdr.detectChanges();
  }

  proceedWithAction(): void {
    this.showConfirmModal = false;
    if (this.actionType === 'delete') {
      if (this.userToDelete) {
        this.executeDeleteUser(this.userToDelete.id, this.userToDelete.name);
      } else if (this.usersToDelete.length > 0) {
        this.executeDeleteMultipleUsers(this.usersToDelete);
      }
    } else if (this.actionType === 'disable') {
      if (this.userToDisable) {
        this.executeDisableUser(this.userToDisable.id, this.userToDisable.name);
      } else if (this.usersToDisable.length > 0) {
        this.executeDisableMultipleUsers(this.usersToDisable);
      }
    } else if (this.actionType === 'approve') {
      if (this.userToApprove) {
        this.executeApproveUser(this.userToApprove.id, this.userToApprove.name);
      } else if (this.usersToApprove.length > 0) {
        this.executeApproveMultipleUsers(this.usersToApprove);
      }
    }
    this.cdr.detectChanges();
  }

  private executeDeleteUser(userId: number, userName: string): void {
    this.isDeleting = true;
    this.adminService.deleteUser(userId).subscribe({
      next: (response) => {
        console.log('Xóa người dùng thành công:', response);
        this.modalMessage = `Xóa người dùng "${userName}" thành công!`;
        this.showSuccessModal = true;
        this.loadUsers();
        this.isDeleting = false;
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Lỗi khi xóa người dùng:', error);
        const errorMessage = error.error?.message || error.statusText || 'Unknown Error';
        this.modalMessage = `Có lỗi xảy ra khi xóa người dùng: ${errorMessage}. Vui lòng thử lại!`;
        this.showErrorModal = true;
        this.isDeleting = false;
        this.cdr.detectChanges();
      }
    });
  }

  private executeDeleteMultipleUsers(userIds: number[]): void {
    this.isDeleting = true;
    this.adminService.deleteUsers(userIds).subscribe({
      next: (response) => {
        console.log('Xóa nhiều người dùng thành công:', response);
        this.modalMessage = `Xóa ${userIds.length} người dùng thành công!`;
        this.showSuccessModal = true;
        this.loadUsers();
        this.isDeleting = false;
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Lỗi khi xóa nhiều người dùng:', error);
        const errorMessage = error.error?.message || error.statusText || 'Unknown Error';
        this.modalMessage = `Có lỗi xảy ra khi xóa người dùng: ${errorMessage}. Vui lòng thử lại!`;
        this.showErrorModal = true;
        this.isDeleting = false;
        this.cdr.detectChanges();
      }
    });
  }

  private executeDisableUser(userId: number, userName: string): void {
    this.isDisabling = true;
    this.adminService.disableUser(userId).subscribe({
      next: (response) => {
        console.log('Vô hiệu hóa người dùng thành công:', response);
        this.modalMessage = `Vô hiệu hóa người dùng "${userName}" thành công!`;
        this.showSuccessModal = true;
        this.loadUsers();
        this.isDisabling = false;
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Lỗi khi vô hiệu hóa người dùng:', error);
        const errorMessage = error.error?.message || error.statusText || 'Unknown Error';
        this.modalMessage = `Có lỗi xảy ra khi vô hiệu hóa người dùng: ${errorMessage}. Vui lòng thử lại!`;
        this.showErrorModal = true;
        this.isDisabling = false;
        this.cdr.detectChanges();
      }
    });
  }

  private executeDisableMultipleUsers(userIds: number[]): void {
    this.isDisabling = true;
    this.adminService.disableUsers(userIds).subscribe({
      next: (response) => {
        console.log('Vô hiệu hóa nhiều người dùng thành công:', response);
        this.modalMessage = `Vô hiệu hóa ${userIds.length} người dùng thành công!`;
        this.showSuccessModal = true;
        this.loadUsers();
        this.isDisabling = false;
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Lỗi khi vô hiệu hóa nhiều người dùng:', error);
        const errorMessage = error.error?.message || error.statusText || 'Unknown Error';
        this.modalMessage = `Có lỗi xảy ra khi vô hiệu hóa người dùng: ${errorMessage}. Vui lòng thử lại!`;
        this.showErrorModal = true;
        this.isDisabling = false;
        this.cdr.detectChanges();
      }
    });
  }

  private executeApproveUser(userId: number, userName: string): void {
    this.isApproving = true;
    this.adminService.approveOrganizer(userId).subscribe({
      next: (response) => {
        console.log('Phê duyệt người tổ chức thành công:', response);
        this.modalMessage = `Phê duyệt người tổ chức "${userName}" thành công!`;
        this.showSuccessModal = true;
        this.loadUsers();
        this.isApproving = false;
        this.userToApprove = null;
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Lỗi khi phê duyệt người tổ chức:', {
          status: error.status,
          statusText: error.statusText,
          error: error.error,
          url: error.url,
          message: error.message
        });
        const errorMessage = error.error?.message || error.statusText || 'Unknown Error';
        this.modalMessage = `Có lỗi khi phê duyệt người tổ chức: ${errorMessage}. Vui lòng thử lại!`;
        this.showErrorModal = true;
        this.isApproving = false;
        this.userToApprove = null;
        this.cdr.detectChanges();
      }
    });
  }

  private executeApproveMultipleUsers(userIds: number[]): void {
    this.isApproving = true;
    const approveRequests = userIds.map(userId => this.adminService.approveOrganizer(userId));
    forkJoin(approveRequests).subscribe({
      next: (responses) => {
        console.log('Phê duyệt nhiều người tổ chức thành công:', responses);
        this.modalMessage = `Phê duyệt ${userIds.length} người tổ chức thành công!`;
        this.showSuccessModal = true;
        this.loadUsers();
        this.isApproving = false;
        this.usersToApprove = [];
        this.selectedUsers = [];
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Lỗi khi phê duyệt nhiều người tổ chức:', {
          status: error.status,
          statusText: error.statusText,
          error: error.error,
          url: error.url,
          message: error.message
        });
        const errorMessage = error.error?.message || error.statusText || 'Unknown Error';
        this.modalMessage = `Có lỗi khi phê duyệt người tổ chức: ${errorMessage}. Vui lòng thử lại!`;
        this.showErrorModal = true;
        this.isApproving = false;
        this.usersToApprove = [];
        this.cdr.detectChanges();
      }
    });
  }

  cancelAction(): void {
    this.showConfirmModal = false;
    this.userToDelete = null;
    this.usersToDelete = [];
    this.userToDisable = null;
    this.usersToDisable = [];
    this.userToApprove = null;
    this.usersToApprove = [];
    this.actionType = null;
    this.isApproving = false;
    this.cdr.detectChanges();
  }

  closeSuccessModal(): void {
    this.showSuccessModal = false;
    this.modalMessage = '';
    this.cdr.detectChanges();
  }

  closeErrorModal(): void {
    this.showErrorModal = false;
    this.modalMessage = '';
    this.cdr.detectChanges();
  }

  confirmDeleteUser_old = this.confirmDeleteUser;
  confirmDeleteMultipleUsers_old = this.confirmDeleteMultipleUsers;
  proceedWithDelete = this.proceedWithAction;
  cancelDelete = this.cancelAction;
}
