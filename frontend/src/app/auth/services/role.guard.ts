import { Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Injectable({
  providedIn: 'root'
})
export class RoleGuard implements CanActivate {
  constructor(private authService: AuthService, private router: Router) {}

  canActivate(route: ActivatedRouteSnapshot): boolean {
    const expectedRole = route.data['expectedRole']; // Lấy role từ route
    const role = this.authService.getRole(); // Lấy role từ JWT

    if (role === expectedRole || (Array.isArray(role) && role.includes(expectedRole))) {
      return true;
    }

    this.router.navigate(['/']); // Nếu không có quyền, về home
    return false;
  }
}
