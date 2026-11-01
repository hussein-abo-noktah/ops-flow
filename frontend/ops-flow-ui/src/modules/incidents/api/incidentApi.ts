import type {Incident, IncidentSearchParams, PagedModel, PageResponse} from "@/modules/incidents/types/incident.types.ts";
import {apiClient} from "@/shared/api/apiClient.ts";
import type {IncidentAction} from '@/modules/incidents/utils/incidentList.ts';
import type {IncidentComment, IncidentTimelineEvent, Severity} from '@/modules/incidents/types/incident.types.ts';
import type {DirectoryEntry, IncidentAssignmentChanges, IncidentDirectoryResource} from '@/modules/incidents/types/incident.types.ts';

const incidentUrl = (id: string) => `/incidents/${encodeURIComponent(id)}`;

export async function getIncidentDirectory(resource: IncidentDirectoryResource, signal?: AbortSignal): Promise<DirectoryEntry[]> {
    const entries: DirectoryEntry[] = [];
    let page = 0;
    while (true) {
        signal?.throwIfAborted();
        const {data} = await apiClient.get<PagedModel<DirectoryEntry>>(`/${resource}`, {params: {page, size: 100}, signal});
        entries.push(...data.content);
        if (++page >= data.page.totalPages) return entries;
    }
}

export async function getIncident(id: string, signal?: AbortSignal): Promise<Incident> {
    return (await apiClient.get<Incident>(incidentUrl(id), {signal})).data;
}

export async function assignIncident(id: string, changes: IncidentAssignmentChanges): Promise<Incident> {
    // The API replaces both assignees. Read at save time and preserve untouched fields.
    const latest = await getIncident(id);
    return changeIncident(id, {
        kind: 'assign',
        teamId: changes.teamId === undefined ? latest.assignedTeamId ?? null : changes.teamId,
        userId: changes.userId === undefined ? latest.assignedUserId ?? null : changes.userId,
    });
}

export async function getIncidentTimeline(id: string, page: number, signal?: AbortSignal) {
    return (await apiClient.get<PagedModel<IncidentTimelineEvent>>(`${incidentUrl(id)}/timeline`, {params: {page, size: 20, direction: 'DESC'}, signal})).data;
}

export async function getIncidentComments(id: string, page: number, signal?: AbortSignal) {
    return (await apiClient.get<PagedModel<IncidentComment>>(`${incidentUrl(id)}/comments`, {params: {page, size: 20}, signal})).data;
}

export type IncidentChange =
    | {kind: 'details'; title: string; description: string}
    | {kind: 'severity'; severity: Severity}
    | {kind: 'assign'; teamId: string | null; userId: string | null}
    | {kind: 'workflow'; action: IncidentAction};

export async function changeIncident(id: string, change: IncidentChange): Promise<Incident> {
    if (change.kind === 'workflow') return transitionIncident(id, change.action);
    const {kind, ...body} = change;
    return (kind === 'details'
        ? await apiClient.put<Incident>(incidentUrl(id), body)
        : await apiClient.post<Incident>(`${incidentUrl(id)}/${kind}`, body)).data;
}

export async function saveIncidentComment(id: string, comment: string, existing?: Pick<IncidentComment, 'id' | 'version'>): Promise<IncidentComment> {
    const url = `${incidentUrl(id)}/comments`;
    return (existing
        ? await apiClient.put<IncidentComment>(`${url}/${encodeURIComponent(existing.id)}`, {comment, version: existing.version})
        : await apiClient.post<IncidentComment>(url, {comment})).data;
}


export async function getIncidents(params: IncidentSearchParams, signal?: AbortSignal): Promise<PageResponse<Incident>> {
    const {data} = await apiClient.get<PagedModel<Incident>>('/incidents', {params, signal});
    return {
        items: data.content, page: data.page.number, size: data.page.size,
        totalItems: data.page.totalElements, totalPages: data.page.totalPages,
        first: data.page.number === 0, last: data.page.number + 1 >= data.page.totalPages,
    };
}


export async function transitionIncident(id: string, action: IncidentAction): Promise<Incident> {
    return (await apiClient.post<Incident>(`/incidents/${encodeURIComponent(id)}/${action}`)).data;
}
