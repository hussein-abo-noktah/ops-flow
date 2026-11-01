import {useState, type SubmitEvent} from 'react';
import type {IncidentComment} from '@/modules/incidents/types/incident.types.ts';
import {useSaveIncidentComment} from '@/modules/incidents/hooks/useIncident.ts';
import {normalizeApiError} from '@/shared/api/normalizeApiError.ts';
import {IncidentError} from '@/modules/incidents/component/IncidentDetailUI.tsx';

export function IncidentCommentEditor({incidentId, existing, latest, onDone}: {
    incidentId: string; existing?: IncidentComment; latest?: IncidentComment; onDone?: () => void;
}) {
    const [text, setText] = useState(existing?.comment ?? '');
    const [base, setBase] = useState(existing ? {id: existing.id, version: existing.version, comment: existing.comment} : undefined);
    const save = useSaveIncidentComment(incidentId);
    const conflict = save.isError && normalizeApiError(save.error).status === 409;
    const newerVersion = base && latest && latest.version > base.version;
    const dirty = Boolean(text.trim()) && (!base || text.trim() !== base.comment);
    function submit(event: SubmitEvent<HTMLFormElement>) {
        event.preventDefault();
        if (!dirty || save.isPending || conflict) return;
        save.mutate({comment: text.trim(), existing: base}, {onSuccess: () => {setText(''); onDone?.();}});
    }
    return <form className="incident-detail-form" onSubmit={submit}>
        <label>{existing ? 'Edit comment' : 'Add a comment'}<textarea required maxLength={10000} rows={3} value={text} onChange={event => {if (!conflict) save.reset(); setText(event.target.value);}} disabled={save.isPending} /></label>
        {save.isError && <IncidentError error={save.error} conflictMessage="This comment changed while you were editing. Your draft is preserved. Review the latest comment before retrying." />}
        {newerVersion && <div><p>Latest comment (version {latest.version}):</p><p className="incident-detail-text">{latest.comment}</p>
            <button type="button" className="incident-button" disabled={save.isPending} onClick={() => {setBase({id: latest.id, version: latest.version, comment: latest.comment}); save.reset();}}>Keep draft and use latest version ({latest.version})</button>
        </div>}
        <div className="incident-detail-actions">
            <button className="incident-button" disabled={save.isPending || !dirty || conflict}>{save.isPending ? 'Saving comment…' : existing ? 'Save comment' : 'Post comment'}</button>
            <button type="button" className="incident-button" disabled={save.isPending} onClick={() => {
                const current = latest ?? existing;
                setText(current?.comment ?? '');
                setBase(current ? {id: current.id, version: current.version, comment: current.comment} : undefined);
                save.reset();
            }}>{existing ? 'Reset to latest' : 'Clear draft'}</button>
            {onDone && <button type="button" className="incident-button" disabled={save.isPending} onClick={onDone}>Cancel</button>}
        </div>
        {save.isSuccess && !existing && <p role="status">Comment posted.</p>}
    </form>;
}
