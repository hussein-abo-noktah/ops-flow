export interface DirectoryEntry {
    id: string;
    name?: string;
    firstName?: string;
    lastName?: string;
    email?: string;
    status?: string;
}

export type IncidentDirectoryResource = 'teams' | 'users';

export interface IncidentAssignmentChanges {
    teamId?: string | null;
    userId?: string | null;
}

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
    createdByName: string;
    createdAt: string;
    updatedAt: string;
    version: number;
}

export interface IncidentComment {
    id: string;
    incidentId: string;
    userId: string;
    comment: string;
    edited: boolean;
    version: number;
    createdAt: string;
    updatedAt: string;
}

export interface IncidentTimelineEvent {
    id: string;
    incidentId: string;
    eventType: string;
    actorId: string | null;
    actorName: string | null;
    oldValue: string | null;
    newValue: string | null;
    metadata: string | null;
    createdAt: string;
    commentId: string | null;
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
