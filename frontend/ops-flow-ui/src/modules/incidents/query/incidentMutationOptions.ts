import type {QueryClient} from '@tanstack/react-query';
import type {Incident} from '@/modules/incidents/types/incident.types.ts';
import {incidentKeys} from '@/modules/incidents/query/incidentKeys.ts';

export function incidentMutationOptions<T>(client: QueryClient, id: string, mutationFn: (variables: T) => Promise<Incident>) {
    return {
        scope: {id: `incident:${id}`},
        mutationFn: async (variables: T) => {
            await client.cancelQueries({queryKey: incidentKeys.detail(id), exact: true});
            return mutationFn(variables);
        },
        onSuccess: async (incident: Incident) => {
            await client.cancelQueries({queryKey: incidentKeys.detail(id), exact: true});
            client.setQueryData<Incident>(incidentKeys.detail(id), current =>
                current && current.version > incident.version ? current : incident);
        },
        onSettled: async () => {
            await Promise.all([
                client.invalidateQueries({queryKey: incidentKeys.detail(id)}),
                client.invalidateQueries({queryKey: incidentKeys.lists()}),
            ]);
        },
    };
}
