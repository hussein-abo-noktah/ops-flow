import {useState, type SubmitEvent} from 'react';
import type {DirectoryEntry, Incident, IncidentAssignmentChanges} from '@/modules/incidents/types/incident.types.ts';
import {useIncidentAssignment, useIncidentDirectory} from '@/modules/incidents/hooks/useIncident.ts';
import {directoryEntryName} from '@/modules/incidents/utils/incidentDetail.ts';
import {IncidentError, IncidentMutationFeedback} from '@/modules/incidents/component/IncidentDetailUI.tsx';

function AssigneeSelect({label, value, entries, disabled, onChange}: {
    label: string; value: string | null; entries?: DirectoryEntry[]; disabled: boolean; onChange: (value: string | null) => void;
}) {
    const found = entries?.some(entry => entry.id === value);
    return <label>{label}<select value={value ?? ''} disabled={disabled || !entries} onChange={event => onChange(event.target.value || null)}>
        <option value="">Unassigned</option>
        {value && !found && <option value={value}>{value}</option>}
        {entries?.filter(entry => entry.status === 'ACTIVE' || entry.id === value).map(entry => <option key={entry.id} value={entry.id}>{directoryEntryName(entry)}</option>)}
    </select></label>;
}

export function IncidentAssignment({incident, canAssign}: {incident: Incident; canAssign: boolean}) {
    const teams = useIncidentDirectory('teams');
    const users = useIncidentDirectory('users');
    const save = useIncidentAssignment(incident.id);
    // Untouched fields follow fresh query data, rather than a snapshot captured at mount.
    const [changes, setChanges] = useState<IncidentAssignmentChanges>({});
    const teamId = changes.teamId === undefined ? incident.assignedTeamId ?? null : changes.teamId;
    const userId = changes.userId === undefined ? incident.assignedUserId ?? null : changes.userId;
    const team = teams.data?.find(entry => entry.id === incident.assignedTeamId);
    const user = users.data?.find(entry => entry.id === incident.assignedUserId);
    const dirty = teamId !== (incident.assignedTeamId ?? null) || userId !== (incident.assignedUserId ?? null);
    function submit(event: SubmitEvent<HTMLFormElement>) {
        event.preventDefault();
        if (!dirty || save.isPending) return;
        const patch: IncidentAssignmentChanges = {};
        if (teamId !== (incident.assignedTeamId ?? null)) patch.teamId = teamId;
        if (userId !== (incident.assignedUserId ?? null)) patch.userId = userId;
        save.mutate(patch, {onSuccess: () => setChanges({})});
    }
    return <>
        <dl className="incident-detail-meta"><div><dt>Assigned team</dt><dd>{team ? directoryEntryName(team) : incident.assignedTeamId || 'Unassigned'}</dd></div><div><dt>Assigned user</dt><dd>{user ? directoryEntryName(user) : incident.assignedUserId || 'Unassigned'}</dd></div></dl>
        {teams.isError && <IncidentError error={teams.error} retry={() => void teams.refetch()} />}
        {users.isError && <IncidentError error={users.error} retry={() => void users.refetch()} />}
        {canAssign && <form className="incident-detail-form" onSubmit={submit}>
            <AssigneeSelect label="Assign team" value={teamId} entries={teams.data} disabled={save.isPending} onChange={value => {save.reset(); setChanges(current => ({...current, teamId: value}));}} />
            <AssigneeSelect label="Assign user" value={userId} entries={users.data} disabled={save.isPending} onChange={value => {save.reset(); setChanges(current => ({...current, userId: value}));}} />
            <IncidentMutationFeedback error={save.error} success={save.isSuccess} message="Assignment saved." />
            <div className="incident-detail-actions"><button className="incident-button" disabled={!dirty || save.isPending}>{save.isPending ? 'Saving assignment…' : 'Save assignment'}</button><button type="button" className="incident-button" disabled={save.isPending} onClick={() => {setChanges({}); save.reset();}}>Reset assignment</button></div>
        </form>}
    </>;
}
