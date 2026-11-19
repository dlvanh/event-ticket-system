import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
@Component({
  selector: 'app-loading',
  imports: [CommonModule],
  templateUrl:'./loading.component.html',
  styleUrls: ['./loading.component.css']
})
export class LoadingComponent {
  @Input() show: boolean = false;
}
