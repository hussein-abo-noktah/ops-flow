const apiUrl =
    import.meta.env.VITE_API_URL || '/api';

export const env = {
    apiUrl,
} as const;
