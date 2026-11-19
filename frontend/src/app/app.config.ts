import { ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { tokenInterceptor } from './auth/services/http-interceptor/token.interceptor'; // Đảm bảo đúng path

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(
      withFetch(), // vẫn dùng Fetch API
      withInterceptors([tokenInterceptor]) // thêm interceptor vào đây
    )
  ]
};
