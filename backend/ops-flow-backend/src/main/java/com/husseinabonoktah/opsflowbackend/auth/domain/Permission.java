package com.husseinabonoktah.opsflowbackend.auth.domain;


import com.husseinabonoktah.opsflowbackend.shared.persistence.BaseEntity;
import jakarta.persistence.*;


@Entity
@Table(name = "permissions")
public class Permission extends BaseEntity {

    @Column(nullable = false, unique = true, length = 150)
    private String code;

    @Column(columnDefinition = "TEXT")
    private String description;

    protected Permission() {
    }
}
