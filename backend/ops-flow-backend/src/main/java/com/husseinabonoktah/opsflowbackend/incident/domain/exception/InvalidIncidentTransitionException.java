package com.husseinabonoktah.opsflowbackend.incident.domain.exception;

import com.husseinabonoktah.opsflowbackend.incident.domain.IncidentStatus;
import com.husseinabonoktah.opsflowbackend.shared.exception.ResourceConflictException;

public class InvalidIncidentTransitionException extends ResourceConflictException {
    public InvalidIncidentTransitionException(IncidentStatus current, IncidentStatus target) {
        super("Cannot transition incident from " + current + " to " + target + ".");
    }
}
