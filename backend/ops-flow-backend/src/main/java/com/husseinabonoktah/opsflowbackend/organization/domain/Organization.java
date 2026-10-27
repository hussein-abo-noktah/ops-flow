package com.husseinabonoktah.opsflowbackend.organization.domain;

import com.husseinabonoktah.opsflowbackend.shared.persistence.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;



@Entity
@Getter
@Setter
@Table(name = "organizations")
public class Organization extends AuditableEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrganizationStatus status;

}
