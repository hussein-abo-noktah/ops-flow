import type {Incident, Severity} from '@/modules/incidents/types/incident.types.ts';
import type {incidentCapabilities} from '@/modules/incidents/utils/incidentDetail.ts';
import {incidentSeverities} from '@/modules/incidents/utils/incidentList.ts';
import {useIncidentChange} from '@/modules/incidents/hooks/useIncident.ts';
import {IncidentAssignment} from '@/modules/incidents/component/IncidentAssignment.tsx';
import {IncidentMutationFeedback, IncidentSection} from '@/modules/incidents/component/IncidentDetailUI.tsx';

function IncidentSeverityControl({incident}: {incident: Incident}) {
    const save = useIncidentChange(incident.id);
    return <>
        <label>Severity<select value={incident.severity} disabled={save.isPending} onChange={event => save.mutate({kind: 'severity', severity: event.target.value as Severity})}>
            {incidentSeverities.map(severity => <option key={severity}>{severity}</option>)}
        </select></label>
        {save.isPending && <p role="status">Updating severity…</p>}
        <IncidentMutationFeedback error={save.error} success={save.isSuccess} message="Severity updated." />
    </>;
}

function IncidentWorkflow({incident, workflow}: {incident: Incident; workflow: ReturnType<typeof incidentCapabilities>['workflow']}) {
    const save = useIncidentChange(incident.id);
    return <>
        <h3>Workflow actions</h3>
        {workflow ? <button className="incident-button" disabled={save.isPending} onClick={() => save.mutate({kind: 'workflow', action: workflow.action})}>{save.isPending ? 'Updating workflow…' : workflow.label}</button>
            : <p>{incident.status === 'CLOSED' ? 'This incident is closed.' : 'No workflow actions available with your permissions.'}</p>}
        <IncidentMutationFeedback error={save.error} success={save.isSuccess} message="Workflow updated." />
    </>;
}

export function IncidentManagement({incident, capabilities}: {incident: Incident; capabilities: ReturnType<typeof incidentCapabilities>}) {
    return <IncidentSection id="incident-management" title="Status & assignment">
        <IncidentAssignment incident={incident} canAssign={capabilities.canAssign} />
        {capabilities.canUpdate && <IncidentSeverityControl incident={incident} />}
        <IncidentWorkflow incident={incident} workflow={capabilities.workflow} />
    </IncidentSection>;
}
