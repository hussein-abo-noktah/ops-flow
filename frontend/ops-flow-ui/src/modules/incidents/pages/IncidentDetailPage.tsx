import {Link, useParams} from 'react-router';
import {useIncidentDetail} from '@/modules/incidents/hooks/useIncident.ts';
import {incidentCapabilities} from '@/modules/incidents/utils/incidentDetail.ts';
import {IncidentDetailHeader} from '@/modules/incidents/component/IncidentDetailHeader.tsx';
import {IncidentOverview} from '@/modules/incidents/component/IncidentOverview.tsx';
import {IncidentManagement} from '@/modules/incidents/component/IncidentManagement.tsx';
import {IncidentTimeline} from '@/modules/incidents/component/IncidentTimeline.tsx';
import {IncidentComments} from '@/modules/incidents/component/IncidentComments.tsx';
import {IncidentError} from '@/modules/incidents/component/IncidentDetailUI.tsx';
import {useAppSelector} from '@/app/store/hooks.ts';
import {selectCurrentUser} from '@/modules/auth/store/authSelectors.ts';
import {normalizeApiError} from '@/shared/api/normalizeApiError.ts';
import './IncidentsPage.css';
import './IncidentDetailPage.css';

export function IncidentDetailPage() {
    const {id = ''} = useParams();
    const query = useIncidentDetail(id);
    const user = useAppSelector(selectCurrentUser);
    const incident = query.data;
    if (query.isPending) return <div className="incident-state" role="status">Loading incident…</div>;
    if (!incident) return <div className="incidents-page">
        <Link to="/incidents">← All incidents</Link>
        <h1>{normalizeApiError(query.error).status === 404 ? 'Incident not found' : 'Unable to load incident'}</h1>
        <IncidentError error={query.error} retry={() => void query.refetch()} />
    </div>;
    const capabilities = incidentCapabilities(user, incident.status);
    return <div key={id} className="incidents-page incident-detail">
        <Link className="incident-back" to="/incidents">← All incidents</Link>
        <IncidentDetailHeader incident={incident} refreshing={query.isFetching} onRefresh={() => void query.refetch()} />
        {query.isError && <IncidentError error={query.error} retry={() => void query.refetch()} />}
        <div className="incident-detail-grid">
            <IncidentOverview incident={incident} canUpdate={capabilities.canUpdate} />
            <IncidentManagement incident={incident} capabilities={capabilities} />
            <IncidentTimeline incidentId={id} />
            <IncidentComments incidentId={id} userId={user?.id} canComment={capabilities.canUpdate} canEdit={capabilities.canEditComment} />
        </div>
    </div>;
}
