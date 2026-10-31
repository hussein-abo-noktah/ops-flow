import type {IncidentSearchParams} from '@/modules/incidents/types/incident.types.ts';
import {incidentSeverities, incidentSorts, incidentStatuses, statusLabel} from '../utils/incidentList.ts';

interface Props {
    params: IncidentSearchParams;
    onChange: (values: Record<string, string>) => void;
}

export function IncidentFilters({params, onChange}: Props) {
    return <div className="incident-filters">
        <form className="incident-search" key={params.q ?? ''} onSubmit={event => {
            event.preventDefault();
            const data = new FormData(event.currentTarget);
            onChange({q: String(data.get('q') ?? '').trim(), search: ''});
        }}>
            <label>Search incidents<input name="q" type="search" defaultValue={params.q ?? ''} maxLength={250} placeholder="Search number, title or description" /></label>
            <button className="incident-button" type="submit">Search</button>
        </form>
        <label>Status<select value={params.status ?? ''} onChange={event => onChange({status: event.target.value})}>
            <option value="">All statuses</option>{incidentStatuses.map(status => <option key={status} value={status}>{statusLabel(status)}</option>)}
        </select></label>
        <label>Severity<select value={params.severity ?? ''} onChange={event => onChange({severity: event.target.value})}>
            <option value="">All severities</option>{incidentSeverities.map(severity => <option key={severity}>{severity}</option>)}
        </select></label>
        <label>Sort by<select value={params.sortBy} onChange={event => onChange({sortBy: event.target.value})}>
            {incidentSorts.map(sort => <option key={sort} value={sort}>{{createdAt: 'Created', updatedAt: 'Updated', incidentNumber: 'Incident number', severity: 'Severity', status: 'Status', title: 'Title'}[sort]}</option>)}
        </select></label>
        <label>Order<select value={params.direction} onChange={event => onChange({direction: event.target.value})}>
            <option value="DESC">Descending</option><option value="ASC">Ascending</option>
        </select></label>
        <button className="incident-button" onClick={() => onChange({q: '', search: '', status: '', severity: '', sortBy: '', direction: ''})}>Reset filters</button>
    </div>;
}
