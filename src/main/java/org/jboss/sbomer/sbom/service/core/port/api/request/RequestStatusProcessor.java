package org.jboss.sbomer.sbom.service.core.port.api.request;

import org.jboss.sbomer.sbom.service.core.domain.enums.RequestStatus;

public interface RequestStatusProcessor {
    void processRequestStatusUpdate(RequestStatus requestStatus);
}
