import { Component, OnInit, OnDestroy, Renderer2, Inject } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { CommonModule } from '@angular/common';
import { RouterModule, RouterOutlet } from '@angular/router'; // Nhập RouterModule
import { FooterComponent } from '../../../component/admin/footer/footer.component';
import { HeaderComponent } from '../../../component/admin/header/header.component';
import { SidebarComponent } from '../../../component/admin/sidebar/sidebar.component';

@Component({
  selector: 'app-home-admin',
  standalone: true, // Đảm bảo component là standalone
  imports: [
    CommonModule,
    RouterModule, // Thêm RouterModule để hỗ trợ routerLink
    RouterOutlet,
    FooterComponent,
    HeaderComponent,
    SidebarComponent,
  ],
  templateUrl: './home-admin.component.html',
  styleUrls: ['./home-admin.component.css'], // Sửa styleUrl thành styleUrls (dạng mảng)
})
export class HomeAdminComponent implements OnInit, OnDestroy {
  private cssFiles = [
    'https://fonts.googleapis.com/css?family=Source+Sans+Pro:300,400,400i,700&display=fallback',
    'plugins/fontawesome-free/css/all.min.css',
    'https://code.ionicframework.com/ionicons/2.0.1/css/ionicons.min.css',
    'plugins/tempusdominus-bootstrap-4/css/tempusdominus-bootstrap-4.min.css',
    'plugins/icheck-bootstrap/icheck-bootstrap.min.css',
    'plugins/jqvmap/jqvmap.min.css',
    'dist/css/adminlte.min.css',
    'dist/css/style.css',
    'plugins/overlayScrollbars/css/OverlayScrollbars.min.css',
    'plugins/daterangepicker/daterangepicker.css',
    'plugins/summernote/summernote-bs4.min.css',
  ];

  private jsFiles = [
    'plugins/jquery/jquery.min.js',
    'plugins/jquery-ui/jquery-ui.min.js',
    'plugins/bootstrap/js/bootstrap.bundle.min.js',
    'plugins/chart.js/Chart.min.js',
    'plugins/sparklines/sparkline.js',
    'plugins/jqvmap/jquery.vmap.min.js',
    'plugins/jqvmap/maps/jquery.vmap.usa.js',
    'plugins/jquery-knob/jquery.knob.min.js',
    'plugins/moment/moment.min.js',
    'plugins/daterangepicker/daterangepicker.js',
    'plugins/tempusdominus-bootstrap-4/js/tempusdominus-bootstrap-4.min.js',
    'plugins/summernote/summernote-bs4.min.js',
    'plugins/overlayScrollbars/js/jquery.overlayScrollbars.min.js',
    'dist/js/adminlte.js',
    'dist/js/demo.js',
    'dist/js/pages/dashboard.js',
  ];

  private elements: HTMLElement[] = [];

  constructor(
    private renderer: Renderer2,
    @Inject(DOCUMENT) private document: Document
  ) {}

  ngOnInit(): void {
    // Tải các tệp CSS
    this.cssFiles.forEach(css => {
      const link = this.renderer.createElement('link');
      this.renderer.setAttribute(link, 'rel', 'stylesheet');
      this.renderer.setAttribute(link, 'href', css);
      this.renderer.appendChild(this.document.head, link);
      this.elements.push(link);
    });

    // Tải các tệp JS
    this.jsFiles.forEach(js => {
      const script = this.renderer.createElement('script');
      this.renderer.setAttribute(script, 'src', js);
      this.renderer.appendChild(this.document.body, script);
      this.elements.push(script);
    });

    // Thêm đoạn mã giải quyết xung đột jQuery UI và Bootstrap tooltip
    const script = this.renderer.createElement('script');
    script.text = `$.widget.bridge('uibutton', $.ui.button);`;
    this.renderer.appendChild(this.document.body, script);
    this.elements.push(script);
  }

  ngOnDestroy(): void {
    // Xóa các phần tử đã thêm để tránh rò rỉ bộ nhớ
    this.elements.forEach(element => {
      this.renderer.removeChild(element.parentNode, element);
    });
    this.elements = [];
  }
}
