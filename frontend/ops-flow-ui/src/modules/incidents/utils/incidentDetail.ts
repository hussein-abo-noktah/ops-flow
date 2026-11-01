import type {AuthUser} from '@/modules/auth/types/auth.types.ts';
import {hasPermission} from '@/modules/auth/utils/permissions.ts';
import type {DirectoryEntry, IncidentComment, IncidentStatus} from '@/modules/incidents/types/incident.types.ts';
import {statusActions} from '@/modules/incidents/utils/incidentList.ts';

export function directoryEntryName(entry: DirectoryEntry) {
    return entry.name || [entry.firstName, entry.lastName].filter(Boolean).join(' ') || entry.email || entry.id;
}

export function timelineEventLabel(eventType: string) {
    return eventType.toLowerCase().replaceAll('_', ' ');
}

export function incidentCapabilities(user: AuthUser | null, status: IncidentStatus) {
    const next = statusActions[status];
    const canUpdate = hasPermission(user, 'incident:update');
    return {
        canUpdate,
        canAssign: hasPermission(user, 'incident:assign'),
        workflow: next && hasPermission(user, next.permission) ? next : undefined,
        canEditComment: (comment: IncidentComment) => canUpdate && user?.id === comment.userId,
    };
}
