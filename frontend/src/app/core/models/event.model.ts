export interface EventCardItem {
  id: number;
  eventName: string;
  description?: string;
  startTime: string;
  endTime?: string;
  category?: string;
  status?: string;
  approvalStatus?: string;
  backgroundUrl?: string;
  address?: string;
  addressDetail?: string;
  price: number;
}

export interface RecommendedEventsPage {
  listEvents: Record<string, any>[];
  totalElements?: number;
  totalPages?: number;
  currentPage?: number;
}
