export interface UserProfile {
  id: number;
  fullName: string;
  email: string;
  phoneNumber?: string;
  gender?: string;
  address?: string;
  role: string;
  status: string;
  createdAt?: string;
  updatedAt?: string;
  avatarUrl?: string;
  bio?: string;
}
