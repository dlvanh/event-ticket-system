export interface ApiResponse<T = unknown> {
  status: number;
  message: string;
  data: T;
  errors?: Record<string, string> | string[];
  timestamp?: string;
}
