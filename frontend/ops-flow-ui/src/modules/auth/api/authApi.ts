import type {AuthResponse, LoginRequest} from "@/modules/auth/types/auth.types.ts";
import {authClient} from "@/shared/api/authClient.ts";
import {normalizeApiError} from "@/shared/api/normalizeApiError.ts";
import {ApiError} from "@/shared/api/apiError.ts";

const refreshTokenKey = 'opsflow.refreshToken';


export async function login(data: LoginRequest): Promise<AuthResponse> {
    try {
        const response = await authClient.post<AuthResponse>("/auth/login", data);
        sessionStorage.setItem(refreshTokenKey, response.data.refreshToken);
        return response.data;
    } catch (error) {
        throw normalizeApiError(error);
    }
}
export async function refreshSession():
    Promise<AuthResponse> {

    try {
        const refreshToken = sessionStorage.getItem(refreshTokenKey);
        if (!refreshToken) {
            throw new ApiError(401, 'NO_SESSION', 'Please sign in to continue.');
        }
        const response =
            await authClient.post<AuthResponse>(
                '/auth/refresh',
                {refreshToken}
            );

        sessionStorage.setItem(refreshTokenKey, response.data.refreshToken);
        return response.data;
    }catch (error) {
        const apiError = normalizeApiError(error);
        if (apiError.status === 400 || apiError.status === 401) {
            sessionStorage.removeItem(refreshTokenKey);
        }

        throw apiError;
    }

}

