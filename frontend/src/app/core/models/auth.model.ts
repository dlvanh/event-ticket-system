export interface LoginResponseDto {
  token: string;
  userId?: number;
  role?: string;
  fullName?: string;
}

export interface DecodedJwtPayload {
  sub?: string;
  role?: string;
  full_name?: string;
  exp?: number;
  iat?: number;
}

export interface OrganizerRegistrationPayload {
  email: string;
  name: string;
  bio: string;
  password: string;
  confirmPassword: string;
  phoneNumber: string;
}
