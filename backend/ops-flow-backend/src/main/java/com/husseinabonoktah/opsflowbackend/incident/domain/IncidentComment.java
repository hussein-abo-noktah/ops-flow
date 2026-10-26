package com.husseinabonoktah.opsflowbackend.incident.domain;


import com.husseinabonoktah.opsflowbackend.user.domain.User;
import com.husseinabonoktah.opsflowbackend.shared.persistence.AuditableEntity;
import jakarta.persistence.*;


@Entity
@Table(name = "incident_comments")
public class IncidentComment extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String comment;

    @Column(name = "is_edited", nullable = false)
    private boolean edited;


    protected IncidentComment() {
    }
}
