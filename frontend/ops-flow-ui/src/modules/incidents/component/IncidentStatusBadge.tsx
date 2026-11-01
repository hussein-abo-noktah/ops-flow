import type {IncidentStatus} from '@/modules/incidents/types/incident.types.ts';
import {statusLabel} from '@/modules/incidents/utils/incidentList.ts';

export function IncidentStatusBadge({status}: {status: IncidentStatus}) {
    return <span className={`incident-status status-${status.toLowerCase()}`}>{statusLabel(status)}</span>;
}
