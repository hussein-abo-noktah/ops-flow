package com.husseinabonoktah.opsflowbackend.incident.domain;

public enum TimelineEventType {

    INCIDENT_CREATED,

    STATUS_CHANGED,

    ASSIGNED,

    SEVERITY_CHANGED,

    COMMENT_ADDED,

    SLA_WARNING,

    RESOLVED
}