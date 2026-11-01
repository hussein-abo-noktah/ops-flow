import type {PropsWithChildren, ReactNode} from 'react';
import type {UseQueryResult} from '@tanstack/react-query';
import type {PagedModel} from '@/modules/incidents/types/incident.types.ts';
import {normalizeApiError} from '@/shared/api/normalizeApiError.ts';
import {PaginationProps as Pagination} from '@/shared/components/PaginationProps.tsx';

export function IncidentTimestamp({value}: {value: string}) {
    return <time dateTime={value}>{new Date(value).toLocaleString()}</time>;
}

export function IncidentError({error, retry, conflictMessage}: {error: unknown; retry?: () => void; conflictMessage?: string}) {
    const normalized = normalizeApiError(error);
    return <div className="incident-error" role="alert">
        <p>{normalized.status === 409 && conflictMessage ? conflictMessage : normalized.message}</p>
        {retry && <button type="button" className="incident-button" onClick={retry}>Reload latest data</button>}
    </div>;
}

export function IncidentMutationFeedback({error, success, message}: {error: unknown; success: boolean; message: string}) {
    return <>{error && <IncidentError error={error} />}{success && <p role="status">{message}</p>}</>;
}

export function IncidentSection({id, title, children}: PropsWithChildren<{id: string; title: string}>) {
    return <section className="incident-panel incident-detail-section" aria-labelledby={id}><h2 id={id}>{title}</h2>{children}</section>;
}

export function IncidentHistory<T>({query, label, onPageChange, children}: {
    query: UseQueryResult<PagedModel<T>, Error>;
    label: string;
    onPageChange: (page: number) => void;
    children: (items: T[]) => ReactNode;
}) {
    const {data} = query;
    return <>
        {query.isPending && <p role="status">Loading {label}…</p>}
        {query.isError && <IncidentError error={query.error} retry={() => void query.refetch()} />}
        {data && <>
            {query.isFetching && <p role="status">Refreshing {label}…</p>}
            {!data.content.length ? <p>No {label}{label === 'timeline' ? ' events' : ''} yet.</p> : children(data.content)}
            <Pagination page={data.page.number} totalPages={data.page.totalPages} first={data.page.number === 0} last={data.page.number + 1 >= data.page.totalPages} disabled={query.isFetching} onPageChange={onPageChange} />
        </>}
    </>;
}
