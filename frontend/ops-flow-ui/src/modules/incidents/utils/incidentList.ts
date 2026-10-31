import type {IncidentSearchParams, IncidentStatus, Severity} from '@/modules/incidents/types/incident.types.ts';

export const incidentStatuses: IncidentStatus[] = ['OPEN', 'ACKNOWLEDGED', 'INVESTIGATING', 'MITIGATED', 'RESOLVED', 'CLOSED'];
export const incidentSeverities: Severity[] = ['SEV1', 'SEV2', 'SEV3', 'SEV4'];
export const incidentSorts = ['createdAt', 'updatedAt', 'incidentNumber', 'severity', 'status', 'title'] as const;
export const pageSizes = [10, 20, 50, 100];

type ResolvedIncidentParams =
    IncidentSearchParams
    & Required<Pick<IncidentSearchParams, 'page' | 'size' | 'sortBy' | 'direction'>>;

function integer(value: string | null, fallback: number, min: number, max: number) {
    if (!value || !/^\d+$/.test(value)) return fallback;
    const parsed = Number(value);
    return Number.isSafeInteger(parsed) && parsed >= min && parsed <= max ? parsed : fallback;
}


function member<T extends string>(values: readonly T[], value: string | null): T | undefined {
    return values.find(candidate => candidate === value);
}

export function readIncidentParams(params: URLSearchParams): ResolvedIncidentParams {
    const size = integer(params.get('size'), 20, 1, 100);
    return {
        page: integer(params.get('page'), 0, 0, Math.floor(2147483647 / size)),
        size,
        q: (params.get('q') ?? params.get('search') ?? '').trim().slice(0, 250) || undefined,
        status: member(incidentStatuses, params.get('status')),
        severity: member(incidentSeverities, params.get('severity')),
        sortBy: member(incidentSorts, params.get('sortBy')) ?? 'createdAt',
        direction: params.get('direction') === 'ASC' ? 'ASC' : 'DESC',
    };
}

export type IncidentAction = 'acknowledge' | 'start-investigation' | 'mitigate' | 'resolve' | 'close';
export const statusActions: Partial<Record<IncidentStatus, {
    action: IncidentAction;
    label: string;
    permission: string
}>> = {
    OPEN: {action: 'acknowledge', label: 'Acknowledge', permission: 'incident:update'},
    ACKNOWLEDGED: {action: 'start-investigation', label: 'Investigate', permission: 'incident:update'},
    INVESTIGATING: {action: 'mitigate', label: 'Mitigate', permission: 'incident:update'},
    MITIGATED: {action: 'resolve', label: 'Resolve', permission: 'incident:resolve'},
    RESOLVED: {action: 'close', label: 'Close', permission: 'incident:resolve'},
};

export function statusLabel(status: IncidentStatus) {
    return status.charAt(0) + status.slice(1).toLowerCase();
}
