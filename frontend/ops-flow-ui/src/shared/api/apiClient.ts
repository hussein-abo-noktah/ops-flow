import axios, {type AxiosError, type InternalAxiosRequestConfig} from "axios";
import {env} from "@/app/config/config.ts";
import {store} from "@/app/store/store.ts";
import {refreshAccessToken} from "@/shared/api/tokenRefresh.ts";

interface RetryableRequestConfig
    extends InternalAxiosRequestConfig {

    _retry?: boolean;
}
function shouldAttemptRefresh(
    error: AxiosError,
    request: RetryableRequestConfig
) {

    if (
        error.response?.status !== 401
    ) {
        return false;
    }

    if (request._retry) {
        return false;
    }

    return !request.url?.includes(
        '/auth/login'
    );


}

export const apiClient = axios.create({
    baseURL: env.apiUrl,
    headers: {
        'Content-Type':
            'application/json',
    },

    timeout: 15_000,
})
apiClient.interceptors.request.use(
    config => {
        const token = store.getState().auth.accessToken
        if (token) {
            config.headers.Authorization = `Bearer ${token}`
        }
        return config
    }
)


apiClient.interceptors.response.use(
    response => response,

    async (
        error: AxiosError
    ) => {

        const originalRequest =
            error.config as
                RetryableRequestConfig | undefined;

        if (
            !originalRequest
            || !shouldAttemptRefresh(error, originalRequest)
        ) {
            return Promise.reject(error);
        }

        originalRequest._retry = true;

        try {

            const accessToken =
                await refreshAccessToken();

            originalRequest
                .headers
                .Authorization =
                `Bearer ${accessToken}`;

            return apiClient(
                originalRequest
            );

        } catch (refreshError) {

            return Promise.reject(
                refreshError
            );
        }
    }
);


