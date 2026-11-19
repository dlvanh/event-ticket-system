import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'statusTranslate',
  standalone: true
})
export class StatusTranslatePipe implements PipeTransform {

  transform(value: string): string {
    const statusMap: { [key: string]: string } = {
      'approved': 'Đã duyệt',
      'pending': 'Đang chờ',
      'rejected': 'Bị từ chối',
      'paid': 'Thành công',
      'cancelled': 'Đã huỷ',

      };
    return statusMap[value] || value;
  }
}
