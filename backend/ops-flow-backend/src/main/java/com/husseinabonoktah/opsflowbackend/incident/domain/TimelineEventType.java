package com.husseinabonoktah.opsflowbackend.incident.domain;

public enum TimelineEventType {

    INCIDENT_CREATED,

    INCIDENT_UPDATED,

    STATUS_CHANGED,

    ASSIGNED,

    SEVERITY_CHANGED,

    COMMENT_ADDED,

    COMMENT_EDITED,

    SLA_WARNING,

    RESOLVED
}
