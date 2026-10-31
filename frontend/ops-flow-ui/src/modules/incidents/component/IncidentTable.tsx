import type {Incident} from '@/modules/incidents/types/incident.types.ts';
import {IncidentStatusBadge} from '@/modules/incidents/component/IncidentStatusBadge.tsx';
import {IncidentSeverityBadge} from '@/modules/incidents/component/IncidentSeverityBadge.tsx';
import {statusActions} from '@/modules/incidents/utils/incidentList.ts';
import type {IncidentAction} from '@/modules/incidents/utils/incidentList.ts';

interface Props {
    incidents: Incident[];
    canUpdate: boolean;
    canResolve: boolean;
    disabled: boolean;
    pendingId?: string;
    onAction: (incident: Incident, action: IncidentAction) => void;
}

export function IncidentTable({incidents, canUpdate, canResolve, disabled, pendingId, onAction}: Props) {
    const showActions = canUpdate || canResolve;
    return <div className="incident-table-scroll">
        <table className="incident-table">
            <caption className="incident-sr-only">Incidents matching the current search and filters</caption>
            <thead><tr><th scope="col">Incident</th><th scope="col">Title</th><th scope="col">Severity</th><th scope="col">Status</th><th scope="col">Created</th>{showActions && <th scope="col">Actions</th>}</tr></thead>
            <tbody>{incidents.map(incident => {
                const next = statusActions[incident.status];
                const allowed = next && (next.permission === 'incident:resolve' ? canResolve : canUpdate);
                return <tr key={incident.id}>
                    <th scope="row">{incident.incidentNumber}</th>
                    <td className="incident-title"><strong>{incident.title}</strong>{incident.description && <details><summary>Description</summary><p>{incident.description}</p></details>}</td>
                    <td><IncidentSeverityBadge severity={incident.severity} /></td>
                    <td><IncidentStatusBadge status={incident.status} /></td>
                    <td><time dateTime={incident.createdAt}>{new Date(incident.createdAt).toLocaleString()}</time></td>
                    {showActions && <td>{allowed ? <button className="incident-button" disabled={disabled} onClick={() => onAction(incident, next.action)} aria-label={`${next.label} ${incident.incidentNumber}`}>{pendingId === incident.id ? 'Updating…' : next.label}</button> : <span aria-label="No available actions">—</span>}</td>}
                </tr>;
            })}</tbody>
        </table>
    </div>;
}
