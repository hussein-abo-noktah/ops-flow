package com.husseinabonoktah.opsflowbackend.auth.security;

public final class Permissions {

    private Permissions() {
    }

    public static final String INCIDENT_CREATE =
            "incident:create";

    public static final String INCIDENT_VIEW =
            "incident:view";

    public static final String INCIDENT_UPDATE =
            "incident:update";

    public static final String INCIDENT_ASSIGN =
            "incident:assign";

    public static final String INCIDENT_RESOLVE =
            "incident:resolve";

    public static final String USER_VIEW =
            "user:view";

    public static final String USER_MANAGE =
            "user:manage";

    public static final String TEAM_CREATE =
            "team:create";

    public static final String TEAM_UPDATE =
            "team:update";

    public static final String AUDIT_VIEW =
            "audit:view";
}