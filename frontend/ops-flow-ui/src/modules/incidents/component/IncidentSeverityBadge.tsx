import type {Severity} from "@/modules/incidents/types/incident.types.ts";

export function IncidentSeverityBadge({
                                          severity,
                                      }: {
    severity: Severity;
}) {

    return (
        <span className={`incident-severity severity-${severity.toLowerCase()}`}>
      {severity}
    </span>
    );
}
