import { Component, OnInit } from '@angular/core';
import { CommonModule} from '@angular/common';
import { StationService } from '../../services/station.service';
import { BatteryStation } from '../../models/station.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})


export class DashboardComponent implements OnInit {
  stations: BatteryStation[] = [];
  isLoading  = true;
  error = '';

  constructor(private stationService: StationService){}

  ngOnInit(): void{
    this.stationService.getStations().subscribe({
      next: (data) => {
        this.stations = data;
        this.isLoading = false;
      },
      error: (err) => {
        this.error = "Failed to load stations";
        this.isLoading = false
      }
    });
  }
}
