import axios from 'axios';
import {ApiError, type ApiErrorResponse} from './apiError.ts';

const httpMessages: Record<number, string> = {
    400: 'Please check your request and try again.',
    401: 'Invalid credentials or token.',
    403: 'You do not have permission to perform this action.',
    404: 'The requested resource was not found.',
    429: 'Too many requests. Please try again later.',
};

export function normalizeApiError(error: unknown): ApiError {
    if (error instanceof ApiError) return error;

    if (axios.isAxiosError<ApiErrorResponse>(error)) {
        const response = error.response;
        // Even an empty or non-JSON HTTP response means the server was reached.
        if (response) {
            const data = response.data && typeof response.data === 'object'
                ? response.data
                : undefined;
            return new ApiError(
                response.status,
                data?.type ?? 'HTTP_ERROR',
                data?.detail || data?.message || httpMessages[response.status]
                    || (response.status >= 500
                        ? 'The server could not complete your request. Please try again later.'
                        : 'Request failed'),
                data?.validationErrors ?? undefined,
            );
        }

        if (error.code === 'ECONNABORTED' || error.code === 'ETIMEDOUT') {
            return new ApiError(0, 'REQUEST_TIMEOUT', 'Request timed out');
        }

        return new ApiError(0, 'NETWORK_ERROR', 'Unable to connect to the server');
    }

    return new ApiError(0, 'UNKNOWN_ERROR', 'An unexpected error occurred');
}
