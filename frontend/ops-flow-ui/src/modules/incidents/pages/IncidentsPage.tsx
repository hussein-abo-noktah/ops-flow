import {useEffect, useState} from 'react';
import {useSearchParams} from 'react-router';
import {useIncident, useIncidentTransition} from '@/modules/incidents/hooks/useIncident.ts';
import {usePermission} from '@/modules/auth/hooks/usePermission.ts';
import {normalizeApiError} from '@/shared/api/normalizeApiError.ts';
import {PaginationProps as Pagination} from '@/shared/components/PaginationProps.tsx';
import {IncidentFilters} from '@/modules/incidents/component/IncidentFilters.tsx';
import {IncidentTable} from '@/modules/incidents/component/IncidentTable.tsx';
import {pageSizes, readIncidentParams, type IncidentAction} from '../utils/incidentList.ts';
import type {Incident} from '@/modules/incidents/types/incident.types.ts';
import './IncidentsPage.css';

export function IncidentsPage() {
    const [searchParams, setSearchParams] = useSearchParams();
    const params = readIncidentParams(searchParams);
    const query = useIncident(params);
    const transition = useIncidentTransition();
    const canUpdate = usePermission('incident:update');
    const canResolve = usePermission('incident:resolve');
    const [notice, setNotice] = useState('');
    const data = query.data;

    function changeParams(values: Record<string, string>) {
        setSearchParams(current => {
            const next = new URLSearchParams(current);
            next.delete('page');
            for (const [key, value] of Object.entries(values)) {
                if (value) next.set(key, value);
                else next.delete(key);
            }
            return next;
        });
    }

    useEffect(() => {
        if (!data || query.isPlaceholderData || query.isError || params.page < data.totalPages || params.page === 0) return;
        setSearchParams(current => {
            const next = new URLSearchParams(current);
            next.set('page', String(Math.max(0, data.totalPages - 1)));
            return next;
        }, {replace: true});
    }, [data, query.isPlaceholderData, query.isError, params.page, setSearchParams]);

    function onAction(incident: Incident, action: IncidentAction) {
        setNotice('');
        transition.mutate({id: incident.id, action}, {
            onSuccess: updated => setNotice(`${updated.incidentNumber} updated successfully.`),
        });
    }

    const filtered = Boolean(params.q || params.status || params.severity);
    return <div className="incidents-page">
        <div className="incident-heading">
            <div className="page-heading"><p className="eyebrow">OPERATIONS</p><h1>Incidents</h1><p>Track, prioritize and resolve operational incidents.</p></div>
        </div>
        {notice && <p className="incident-notice" role="status">{notice}</p>}
        {transition.isError && <div className="incident-error" role="alert">{normalizeApiError(transition.error).message}<button className="incident-button" onClick={() => transition.reset()}>Dismiss</button></div>}
        <section className="incident-panel" aria-label="Incident list" aria-busy={query.isFetching}>
            <IncidentFilters params={params} onChange={changeParams} />
            {query.isPending ? <div className="incident-state" role="status">Loading incidents...</div>
                : query.isError ? <div className="incident-error" role="alert"><p>Failed to load incidents. {normalizeApiError(query.error).message}</p><button className="incident-button" disabled={query.isFetching} onClick={() => void query.refetch()}>Retry</button></div>
                : data && <>
                    <div className="incident-list-meta" role="status"><span>{data.totalItems} {data.totalItems === 1 ? 'incident' : 'incidents'}{filtered ? ' matching your filters' : ''}</span>{query.isFetching && <span>Refreshing…</span>}</div>
                    {data.items.length ? <IncidentTable incidents={data.items} canUpdate={canUpdate} canResolve={canResolve} disabled={transition.isPending || query.isFetching} pendingId={transition.isPending ? transition.variables.id : undefined} onAction={onAction} />
                        : <div className="incident-state"><h2>{filtered ? 'No matching incidents' : 'No incidents yet'}</h2><p>{filtered ? 'Try another search or reset your filters.' : 'New incidents will appear here.'}</p></div>}
                    <div className="incident-pagination">
                        <label>Rows per page<select value={params.size} onChange={event => changeParams({size: event.target.value})}>
                            {[...new Set([...pageSizes, params.size])].sort((a, b) => a - b).map(size => <option key={size}>{size}</option>)}
                        </select></label>
                        <Pagination page={data.page} totalPages={data.totalPages} first={data.first} last={data.last} disabled={query.isFetching} onPageChange={page => changeParams({page: String(page)})} />
                    </div>
                </>}
        </section>
    </div>;
}
