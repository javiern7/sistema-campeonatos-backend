package com.multideporte.backend.security.audit;

import com.multideporte.backend.security.user.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OperationalAuditService {

    private final CurrentUserService currentUserService;

    public void auditSuccess(String action, String resourceType, Object resourceId) {
        log.info(
                "operation_audit outcome=SUCCESS action={} resourceType={} resourceId={} actorUserId={} actorUsername={}",
                action,
                resourceType,
                resourceId,
                currentUserService.getCurrentUserId().orElse(null),
                currentUserService.getCurrentUsername()
        );
    }
}
