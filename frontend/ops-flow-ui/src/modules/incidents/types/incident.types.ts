export type IncidentStatus =
    | 'OPEN'
    | 'ACKNOWLEDGED'
    | 'INVESTIGATING'
    | 'MITIGATED'
    | 'RESOLVED'
    | 'CLOSED';

export type Severity =
    | 'SEV1'
    | 'SEV2'
    | 'SEV3'
    | 'SEV4';

export interface Incident {
    id: string;
    incidentNumber: string;
    title: string;
    description?: string | null;
    severity: Severity;
    status: IncidentStatus;
    assignedTeamId?: string | null;
    assignedUserId?: string | null;
    createdBy: string;
    createdAt: string;
    updatedAt: string;
    version: number;
}

export interface IncidentSearchParams {
    page?: number;
    size?: number;
    q?: string;
    status?: IncidentStatus;
    severity?: Severity;
    assignedTeamId?: string;
    assignedUserId?: string;
    sortBy?: 'createdAt' | 'updatedAt' | 'incidentNumber' | 'severity' | 'status' | 'title';
    direction?: 'ASC' | 'DESC';
}

export interface PagedModel<T> {
    content: T[];
    page: {number: number; size: number; totalElements: number; totalPages: number};
}

export interface PageResponse<T> {
    items: T[];
    page: number;
    size: number;
    totalItems: number;
    totalPages: number;
    first: boolean;
    last: boolean;
}
