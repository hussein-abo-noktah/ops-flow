import {keepPreviousData, useMutation, useQuery, useQueryClient} from "@tanstack/react-query";
import {incidentKeys} from "@/modules/incidents/query/incidentKeys.ts";
import type {IncidentSearchParams} from "@/modules/incidents/types/incident.types.ts";
import {getIncidents, transitionIncident} from "@/modules/incidents/api/incidentApi.ts";
import type {IncidentAction} from '@/modules/incidents/utils/incidentList.ts';

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
            await client.invalidateQueries({queryKey: incidentKeys.lists()});
        },
    });
}
