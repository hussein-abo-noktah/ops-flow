import axios from 'axios';
import {ApiError, type ApiErrorResponse} from "@/shared/api/apiError.ts";



export function normalizeApiError(
    error: unknown
): ApiError {
    if (error instanceof ApiError) {
        return error;
    }

    if (
        axios.isAxiosError<ApiErrorResponse>(
            error
        )
    ) {

        const response = error.response;

        if (response?.data) {

            return new ApiError(
                response.status,
                response.data.type ?? 'HTTP_ERROR',
                response.data.detail || response.data.message || 'Request failed',
                response.data.validationErrors ??
                undefined
            );
        }

        if (error.code === 'ECONNABORTED') {
            return new ApiError(
                0,
                'REQUEST_TIMEOUT',
                'Request timed out'
            );
        }

        return new ApiError(
            0,
            'NETWORK_ERROR',
            'Unable to connect to the server'
        );
    }

    return new ApiError(
        0,
        'UNKNOWN_ERROR',
        'An unexpected error occurred'
    );
}
