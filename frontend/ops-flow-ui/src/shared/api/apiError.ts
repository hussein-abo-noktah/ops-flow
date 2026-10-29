export interface ApiErrorResponse {
    type: string;
    message: string;
    detail?: string;
    status: number;
    path?: string;

    validationErrors?: Record<
        string,
        string
    > | null;
}

export class ApiError extends Error {
    public readonly status: number;
    public readonly type: string;
    public readonly validationErrors?: Record<string, string>;

    constructor(
        status: number,
        type: string,
        message: string,

        validationErrors?: Record<
            string,
            string
        >
    ) {
        super(message);

        this.name = 'ApiError';
        this.status = status;
        this.type = type;
        this.validationErrors = validationErrors;
    }
}
