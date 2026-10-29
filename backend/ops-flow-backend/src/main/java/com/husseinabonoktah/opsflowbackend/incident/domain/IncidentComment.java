package com.husseinabonoktah.opsflowbackend.incident.domain;


import com.husseinabonoktah.opsflowbackend.user.domain.User;
import com.husseinabonoktah.opsflowbackend.shared.persistence.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import java.util.Objects;


@Entity
@Getter
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

    @Version
    @Column(nullable = false)
    private Long version;


    protected IncidentComment() {
    }

    public IncidentComment(Incident incident, User user, String comment) {
        this.incident = Objects.requireNonNull(incident);
        this.user = Objects.requireNonNull(user);
        this.comment = Objects.requireNonNull(comment);
    }

    public void edit(String comment) {
        this.comment = Objects.requireNonNull(comment);
        this.edited = true;
    }
}
