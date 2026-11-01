import type {IncidentDirectoryResource, IncidentSearchParams} from "@/modules/incidents/types/incident.types.ts";

export const incidentKeys = {
    directory: (resource: IncidentDirectoryResource) => ['incident-directory', resource] as const,
    all: ['incidents'] as const,

    lists: () =>
        [...incidentKeys.all, 'list'] as const,

    list: (
        params: IncidentSearchParams
    ) =>
        [
            ...incidentKeys.lists(),
            params,
        ] as const,

    details: () =>
        [...incidentKeys.all, 'detail'] as const,

    detail: (id: string) =>
        [
            ...incidentKeys.details(),
            id,
        ] as const,
    timeline: (id: string, page: number) => [...incidentKeys.detail(id), 'timeline', page] as const,
    comments: (id: string, page: number) => [...incidentKeys.detail(id), 'comments', page] as const,
};
