import { Component, OnInit, OnDestroy, Renderer2, Inject } from '@angular/core';
import { DOCUMENT, CommonModule } from '@angular/common';
import { RouterModule, RouterOutlet } from '@angular/router';
import { FooterComponent } from '../../../component/admin/footer/footer.component';
import { HeaderComponent } from '../../../component/admin/header/header.component';
import { SidebarComponent } from '../../../component/admin/sidebar/sidebar.component';

@Component({
  selector: 'app-home-admin',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    RouterOutlet,
    FooterComponent,
    HeaderComponent,
    SidebarComponent,
  ],
  templateUrl: './home-admin.component.html',
  styleUrls: ['./home-admin.component.css'],
})
export class HomeAdminComponent implements OnInit, OnDestroy {
  private readonly cssFiles = [
    'https://fonts.googleapis.com/css?family=Source+Sans+Pro:300,400,400i,700&display=fallback',
    'https://code.ionicframework.com/ionicons/2.0.1/css/ionicons.min.css',
    'https://cdn.jsdelivr.net/npm/admin-lte@3.2/dist/css/adminlte.min.css',
    'assets/admin-modal.css',
  ];

  private readonly jsFiles = [
    'https://cdn.jsdelivr.net/npm/jquery@3.6.4/dist/jquery.min.js',
    'https://cdn.jsdelivr.net/npm/bootstrap@4.6.2/dist/js/bootstrap.bundle.min.js',
    'https://cdn.jsdelivr.net/npm/admin-lte@3.2/dist/js/adminlte.min.js',
  ];

  private elements: HTMLElement[] = [];

  constructor(
    private renderer: Renderer2,
    @Inject(DOCUMENT) private document: Document
  ) {}

  ngOnInit(): void {
    this.cssFiles.forEach(css => {
      const link = this.renderer.createElement('link');
      this.renderer.setAttribute(link, 'rel', 'stylesheet');
      this.renderer.setAttribute(link, 'href', css);
      this.renderer.appendChild(this.document.head, link);
      this.elements.push(link);
    });

    this.loadScriptsSequentially(0);
  }

  private loadScriptsSequentially(index: number): void {
    if (index >= this.jsFiles.length) return;
    const script = this.renderer.createElement('script');
    this.renderer.setAttribute(script, 'src', this.jsFiles[index]);
    script.onload = () => this.loadScriptsSequentially(index + 1);
    this.renderer.appendChild(this.document.body, script);
    this.elements.push(script);
  }

  ngOnDestroy(): void {
    this.elements.forEach(element => {
      if (element.parentNode) {
        this.renderer.removeChild(element.parentNode, element);
      }
    });
    this.elements = [];
  }
}
