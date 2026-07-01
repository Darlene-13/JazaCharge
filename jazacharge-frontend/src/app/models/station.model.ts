export interface Station {
  id: number;
  name: string;
  location: string;
  availableBatteries: number;
  isActive: boolean;
  activeReservations: number;
}
