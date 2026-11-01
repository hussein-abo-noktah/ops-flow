import type {Incident} from '@/modules/incidents/types/incident.types.ts';
import {IncidentStatusBadge} from '@/modules/incidents/component/IncidentStatusBadge.tsx';
import {IncidentSeverityBadge} from '@/modules/incidents/component/IncidentSeverityBadge.tsx';

export function IncidentDetailHeader({incident, refreshing, onRefresh}: {incident: Incident; refreshing: boolean; onRefresh: () => void}) {
    return <header className="incident-heading">
        <div className="page-heading">
            <p className="eyebrow">{incident.incidentNumber}</p><h1>{incident.title}</h1>
            <div className="incident-detail-actions"><IncidentStatusBadge status={incident.status} /><IncidentSeverityBadge severity={incident.severity} /><span>Version {incident.version}</span>{refreshing && <span role="status">Refreshing…</span>}</div>
        </div>
        <button className="incident-button" disabled={refreshing} onClick={onRefresh}>Refresh</button>
    </header>;
}
