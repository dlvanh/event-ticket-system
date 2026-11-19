import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../../auth/services/auth.service';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const authService = inject(AuthService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && !req.url.includes('/auth/login')) {
        authService.logout();
        if (router.url !== '/login') {
          router.navigate(['/login']);
        }
      } else if (error.status === 403) {
        console.warn('Access denied for request:', req.url);
      } else if (error.status >= 500) {
        console.error('Server error for request:', req.url, error.message);
      }
      return throwError(() => error);
    })
  );
};
