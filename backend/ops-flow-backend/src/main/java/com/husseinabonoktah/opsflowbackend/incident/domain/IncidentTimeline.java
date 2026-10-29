package com.husseinabonoktah.opsflowbackend.incident.domain;

import com.husseinabonoktah.opsflowbackend.user.domain.User;
import com.husseinabonoktah.opsflowbackend.shared.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;


@Entity
@Getter
@org.hibernate.annotations.Immutable
@Table(name = "incident_timeline")
public class IncidentTimeline extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 100)
    private TimelineEventType eventType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String metadata;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", updatable = false)
    private IncidentComment comment;


    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    protected IncidentTimeline() {
    }

    public IncidentTimeline(Incident incident, TimelineEventType eventType, User actor,
                            String oldValue, String newValue) {
        this(incident, eventType, actor, oldValue, newValue, null, null);
    }

    public IncidentTimeline(Incident incident, TimelineEventType eventType, User actor,
                            String oldValue, String newValue, IncidentComment comment, String metadata) {
        this.incident = incident;
        this.eventType = eventType;
        this.actor = actor;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.comment = comment;
        this.metadata = metadata;
    }
}
