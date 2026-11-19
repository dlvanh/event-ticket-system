import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-interest-place',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './interest-place.component.html',
  styleUrl: './interest-place.component.css'
})
export class InterestPlaceComponent {
  constructor(private router: Router) {}

  interestPlaces = [
    {
      name: 'Hồ Chí Minh',
      image: 'assets/HCM.webp'
    },
    {
      name: 'Hà Nội',
      image: 'assets/HNOI.webp'
    },
    {
      name: 'Đà Lạt',
      image: 'assets/DALAT.webp'
    },
    {
      name: 'Vị trí khác',
      image: 'assets/ANOTHER.webp'
    }
  ];

  onCityClick(cityName: string) {
    this.router.navigate(['/search-events'], {
      queryParams: { address: cityName }
    });
  }
}
