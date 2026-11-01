import {useState, type SubmitEvent} from 'react';
import type {Incident} from '@/modules/incidents/types/incident.types.ts';
import {useIncidentChange} from '@/modules/incidents/hooks/useIncident.ts';
import {IncidentError, IncidentSection, IncidentTimestamp} from '@/modules/incidents/component/IncidentDetailUI.tsx';

function DetailsForm({incident, onDone}: {incident: Incident; onDone: () => void}) {
    const [baseline, setBaseline] = useState({title: incident.title, description: incident.description ?? ''});
    const [draft, setDraft] = useState(baseline);
    const save = useIncidentChange(incident.id);
    const changedRemotely = incident.title !== baseline.title || (incident.description ?? '') !== baseline.description;
    const dirty = draft.title.trim() !== baseline.title || draft.description.trim() !== baseline.description;
    function reset() {
        const latest = {title: incident.title, description: incident.description ?? ''};
        setBaseline(latest); setDraft(latest); save.reset();
    }
    function submit(event: SubmitEvent<HTMLFormElement>) {
        event.preventDefault();
        if (save.isPending || changedRemotely || !dirty || !draft.title.trim()) return;
        save.mutate({kind: 'details', title: draft.title.trim(), description: draft.description.trim()}, {onSuccess: onDone});
    }
    return <form className="incident-detail-form" onSubmit={submit}>
        <label>Title<input required maxLength={250} value={draft.title} disabled={save.isPending} onChange={event => {save.reset(); setDraft({...draft, title: event.target.value});}} /></label>
        <label>Description<textarea rows={5} maxLength={10000} value={draft.description} disabled={save.isPending} onChange={event => {save.reset(); setDraft({...draft, description: event.target.value});}} /></label>
        {changedRemotely && <p role="status">Incident details changed while editing. Your draft is preserved. Reset to the latest details before editing again.</p>}
        {save.isError && <IncidentError error={save.error} />}
        <div className="incident-detail-actions">
            <button className="incident-button" disabled={save.isPending || changedRemotely || !dirty || !draft.title.trim()}>{save.isPending ? 'Saving details…' : 'Save details'}</button>
            <button type="button" className="incident-button" disabled={save.isPending} onClick={reset}>Reset to latest</button>
            <button type="button" className="incident-button" disabled={save.isPending} onClick={onDone}>Cancel</button>
        </div>
    </form>;
}

export function IncidentOverview({incident, canUpdate}: {incident: Incident; canUpdate: boolean}) {
    const [editing, setEditing] = useState(false);
    return <IncidentSection id="incident-overview" title="Incident overview">
        {editing && canUpdate ? <DetailsForm incident={incident} onDone={() => setEditing(false)} /> : <>
            <p className="incident-detail-text">{incident.description || 'No description provided.'}</p>
            {canUpdate && <button className="incident-button" onClick={() => setEditing(true)}>Edit details</button>}
        </>}
        <dl className="incident-detail-meta"><div><dt>Created by</dt><dd>{incident.createdByName || 'Unknown user'}</dd></div><div><dt>Created</dt><dd><IncidentTimestamp value={incident.createdAt} /></dd></div><div><dt>Last updated</dt><dd><IncidentTimestamp value={incident.updatedAt} /></dd></div></dl>
    </IncidentSection>;
}
