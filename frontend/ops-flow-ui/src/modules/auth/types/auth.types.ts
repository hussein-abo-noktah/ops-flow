export interface LoginRequest {
    email: string;
    password: string;
}

export interface AuthResponse {
    accessToken: string;
    refreshToken: string;
    tokenType: string;
    expiresIn: number;
}

export interface AuthUser {
    id: string;
    organizationId: string;
    email?: string;
    roles: string[];
    permissions: string[];
}
