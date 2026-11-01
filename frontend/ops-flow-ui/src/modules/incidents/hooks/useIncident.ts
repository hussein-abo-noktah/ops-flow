import {keepPreviousData, useMutation, useQuery, useQueryClient} from "@tanstack/react-query";
import {incidentKeys} from "@/modules/incidents/query/incidentKeys.ts";
import type {IncidentSearchParams} from "@/modules/incidents/types/incident.types.ts";
import {getIncidents, transitionIncident, getIncident, getIncidentTimeline, getIncidentComments, changeIncident, assignIncident, saveIncidentComment, getIncidentDirectory, type IncidentChange} from "@/modules/incidents/api/incidentApi.ts";
import type {Incident, IncidentAssignmentChanges, IncidentComment, IncidentDirectoryResource} from '@/modules/incidents/types/incident.types.ts';
import type {IncidentAction} from '@/modules/incidents/utils/incidentList.ts';
import {incidentMutationOptions} from '@/modules/incidents/query/incidentMutationOptions.ts';

export function useIncidentDirectory(resource: IncidentDirectoryResource, enabled = true) {
    return useQuery({
        queryKey: incidentKeys.directory(resource),
        queryFn: ({signal}) => getIncidentDirectory(resource, signal),
        enabled,
    });
}

export function useIncident(params: IncidentSearchParams) {
    return useQuery({
        queryKey: incidentKeys.list(params),
        queryFn: ({signal}) => getIncidents(params, signal),
        placeholderData: keepPreviousData,
    })
}

export function useIncidentTransition() {
    const client = useQueryClient();
    return useMutation({
        mutationFn: ({id, action}: {id: string; action: IncidentAction}) => transitionIncident(id, action),
        onSuccess: async incident => {
            client.setQueryData(incidentKeys.detail(incident.id), incident);
            await Promise.all([
                client.invalidateQueries({queryKey: incidentKeys.lists()}),
                client.invalidateQueries({queryKey: incidentKeys.detail(incident.id)}),
            ]);
        },
    });
}

export function useIncidentDetail(id: string) {
    return useQuery({queryKey: incidentKeys.detail(id), queryFn: ({signal}) => getIncident(id, signal), enabled: Boolean(id)});
}

export function useIncidentTimeline(id: string, page: number) {
    return useQuery({queryKey: incidentKeys.timeline(id, page), queryFn: ({signal}) => getIncidentTimeline(id, page, signal), enabled: Boolean(id)});
}

export function useIncidentComments(id: string, page: number) {
    return useQuery({queryKey: incidentKeys.comments(id, page), queryFn: ({signal}) => getIncidentComments(id, page, signal), enabled: Boolean(id)});
}

export function useIncidentChange(id: string) {
    return useIncidentMutation(id, (change: IncidentChange) => changeIncident(id, change));
}

export function useIncidentAssignment(id: string) {
    return useIncidentMutation(id, (changes: IncidentAssignmentChanges) => assignIncident(id, changes));
}

function useIncidentMutation<T>(id: string, mutationFn: (variables: T) => Promise<Incident>) {
    const client = useQueryClient();
    return useMutation(incidentMutationOptions(client, id, mutationFn));
}

export function useSaveIncidentComment(id: string) {
    const client = useQueryClient();
    return useMutation({
        mutationFn: ({comment, existing}: {comment: string; existing?: Pick<IncidentComment, 'id' | 'version'>}) => saveIncidentComment(id, comment, existing),
        onSettled: async () => { await client.invalidateQueries({queryKey: incidentKeys.detail(id)}); },
    });
}
