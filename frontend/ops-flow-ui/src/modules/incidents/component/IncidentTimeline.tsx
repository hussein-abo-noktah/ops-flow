import {useState} from 'react';
import {useIncidentTimeline} from '@/modules/incidents/hooks/useIncident.ts';
import {timelineEventLabel} from '@/modules/incidents/utils/incidentDetail.ts';
import {IncidentHistory, IncidentSection, IncidentTimestamp} from '@/modules/incidents/component/IncidentDetailUI.tsx';

export function IncidentTimeline({incidentId}: { incidentId: string }) {
    const [page, setPage] = useState(0);
    const query = useIncidentTimeline(incidentId, page);
    return <IncidentSection id="incident-timeline" title="Timeline">
        <IncidentHistory query={query} label="timeline" onPageChange={setPage}>{events => <ol
            className="incident-timeline">
            {events.map(event => <li key={event.id}>
                <strong>{timelineEventLabel(event.eventType)}</strong><IncidentTimestamp value={event.createdAt}/>
                <p>By {event.actorName || (event.actorId ? 'Unknown user' : 'System')}</p>
                {(event.oldValue || event.newValue) &&
                    <p className="incident-detail-text">{event.oldValue || '—'} → {event.newValue || '—'}</p>}
            </li>)}
        </ol>}</IncidentHistory>
    </IncidentSection>;
}
