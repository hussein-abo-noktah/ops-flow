import type {Incident, IncidentSearchParams, PagedModel, PageResponse} from "@/modules/incidents/types/incident.types.ts";
import {apiClient} from "@/shared/api/apiClient.ts";
import type {IncidentAction} from '../utils/incidentList.ts';


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
