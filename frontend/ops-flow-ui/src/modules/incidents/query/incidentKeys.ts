import type {IncidentSearchParams} from "@/modules/incidents/types/incident.types.ts";

export const incidentKeys = {
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
};