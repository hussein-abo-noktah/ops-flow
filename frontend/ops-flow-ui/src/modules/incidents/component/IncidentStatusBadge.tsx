import type {IncidentStatus} from '../types/incident.types.ts';
import {statusLabel} from '../utils/incidentList.ts';

export function IncidentStatusBadge({status}: {status: IncidentStatus}) {
    return <span className={`incident-status status-${status.toLowerCase()}`}>{statusLabel(status)}</span>;
}
