package com.multideporte.backend.security.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.multideporte.backend.security.audit.dto.OperationalActivitySummaryResponse;
import com.multideporte.backend.security.audit.dto.OperationalAuditEventResponse;
import com.multideporte.backend.security.auth.AuthSessionAuthenticationDetails;
import com.multideporte.backend.security.user.CurrentUserService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
@RequiredArgsConstructor
@Slf4j
public class OperationalAuditService {

    private static final OffsetDateTime MIN_OCCURRENCE = OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime MAX_OCCURRENCE = OffsetDateTime.of(9999, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC);

    private final CurrentUserService currentUserService;
    private final OperationalAuditEventRepository operationalAuditEventRepository;
    private final ObjectMapper objectMapper;

    public void auditSuccess(String action, String resourceType, Object resourceId) {
        audit(action, resourceType, resourceId, null, null, OperationalAuditResult.SUCCESS, null);
    }

    public void auditFailure(String action, String resourceType, Object resourceId, String reasonCode) {
        auditFailure(action, resourceType, resourceId, null, null, reasonCode, null);
    }

    public void auditDenied(String action, String resourceType, Object resourceId, String reasonCode) {
        auditDenied(action, resourceType, resourceId, null, null, reasonCode, null);
    }

    public void auditSuccess(
            String action,
            String resourceType,
            Object resourceId,
            Long actorUserId,
            String actorUsername,
            Map<String, Object> extraContext
    ) {
        audit(action, resourceType, resourceId, actorUserId, actorUsername, OperationalAuditResult.SUCCESS, extraContext);
    }

    public void auditFailure(
            String action,
            String resourceType,
            Object resourceId,
            Long actorUserId,
            String actorUsername,
            String reasonCode,
            Map<String, Object> extraContext
    ) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("reasonCode", reasonCode);
        if (extraContext != null) {
            context.putAll(extraContext);
        }
        audit(action, resourceType, resourceId, actorUserId, actorUsername, OperationalAuditResult.FAILED, context);
    }

    public void auditDenied(
            String action,
            String resourceType,
            Object resourceId,
            Long actorUserId,
            String actorUsername,
            String reasonCode,
            Map<String, Object> extraContext
    ) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("reasonCode", reasonCode);
        if (extraContext != null) {
            context.putAll(extraContext);
        }
        audit(action, resourceType, resourceId, actorUserId, actorUsername, OperationalAuditResult.DENIED, context);
    }

    public Page<OperationalAuditEventResponse> getAuditEvents(
            String action,
            String entityType,
            String actor,
            OperationalAuditResult result,
            OffsetDateTime from,
            OffsetDateTime to,
            Pageable pageable
    ) {
        Pageable normalizedPageable = normalizePageable(pageable);
        return operationalAuditEventRepository.findAll(
                        OperationalAuditSpecifications.byFilters(action, entityType, actor, result, from, to),
                        normalizedPageable
                )
                .map(this::toResponse);
    }

    public OperationalActivitySummaryResponse getActivitySummary(OffsetDateTime from, OffsetDateTime to) {
        OffsetDateTime effectiveFrom = from == null ? MIN_OCCURRENCE : from;
        OffsetDateTime effectiveTo = to == null ? MAX_OCCURRENCE : to;

        long totalEvents = operationalAuditEventRepository.count(
                OperationalAuditSpecifications.byFilters(null, null, null, null, from, to)
        );
        long successEvents = operationalAuditEventRepository.count(
                OperationalAuditSpecifications.byFilters(null, null, null, OperationalAuditResult.SUCCESS, from, to)
        );
        long deniedEvents = operationalAuditEventRepository.count(
                OperationalAuditSpecifications.byFilters(null, null, null, OperationalAuditResult.DENIED, from, to)
        );
        long failedEvents = operationalAuditEventRepository.count(
                OperationalAuditSpecifications.byFilters(null, null, null, OperationalAuditResult.FAILED, from, to)
        );
        long uniqueActors = operationalAuditEventRepository.countDistinctActors(effectiveFrom, effectiveTo);
        List<OperationalActivitySummaryResponse.ActionCountResponse> topActions = operationalAuditEventRepository
                .findActionCounts(effectiveFrom, effectiveTo, PageRequest.of(0, 10))
                .stream()
                .map(item -> new OperationalActivitySummaryResponse.ActionCountResponse(item.action(), item.total()))
                .toList();

        return new OperationalActivitySummaryResponse(
                from,
                to,
                totalEvents,
                successEvents,
                deniedEvents,
                failedEvents,
                uniqueActors,
                topActions
        );
    }

    public List<OperationalAuditEventResponse> getRecentAuditEvents(int limit) {
        PageRequest pageRequest = PageRequest.of(0, limit, Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("id")));
        return operationalAuditEventRepository.findAll(pageRequest)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private void audit(
            String action,
            String resourceType,
            Object resourceId,
            Long actorUserId,
            String actorUsername,
            OperationalAuditResult result,
            Map<String, Object> extraContext
    ) {
        OffsetDateTime occurredAt = OffsetDateTime.now();
        Map<String, Object> context = buildContext(extraContext);
        Long resolvedActorUserId = actorUserId != null ? actorUserId : currentUserService.getCurrentUserId().orElse(null);
        String resolvedActorUsername = actorUsername != null && !actorUsername.isBlank()
                ? actorUsername
                : currentUserService.getCurrentUsername();

        log.info(
                "operation_audit outcome={} action={} resourceType={} resourceId={} actorUserId={} actorUsername={} context={}",
                result,
                action,
                resourceType,
                resourceId,
                resolvedActorUserId,
                resolvedActorUsername,
                context
        );

        try {
            OperationalAuditEvent event = new OperationalAuditEvent();
            event.setActorUserId(resolvedActorUserId);
            event.setActorUsername(truncate(resolvedActorUsername == null ? "anonymous" : resolvedActorUsername, 100));
            event.setAction(truncate(action, 100));
            event.setEntityType(truncate(resourceType, 100));
            event.setEntityId(resourceId == null ? null : truncate(String.valueOf(resourceId), 100));
            event.setOccurredAt(occurredAt);
            event.setResult(result);
            event.setContextJson(writeContext(context));
            operationalAuditEventRepository.save(event);
        } catch (Exception ex) {
            log.warn("No se pudo persistir evento de auditoria operativa action={} resourceType={}", action, resourceType, ex);
        }
    }

    private Pageable normalizePageable(Pageable pageable) {
        if (pageable == null) {
            return PageRequest.of(0, 20, Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("id")));
        }

        Sort sort = pageable.getSort().isSorted()
                ? pageable.getSort().and(Sort.by(Sort.Order.desc("id")))
                : Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("id"));

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }

    private Map<String, Object> buildContext(Map<String, Object> extraContext) {
        Map<String, Object> context = new LinkedHashMap<>();
        HttpServletRequest request = resolveCurrentRequest();
        if (request != null) {
            context.put("requestPath", request.getRequestURI());
            context.put("httpMethod", request.getMethod());
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getDetails() instanceof AuthSessionAuthenticationDetails details) {
            context.put("authenticationScheme", details.authenticationScheme());
            if (details.sessionId() != null) {
                context.put("sessionId", details.sessionId());
            }
        }

        if (extraContext != null) {
            extraContext.forEach((key, value) -> {
                if (value != null) {
                    context.put(key, value);
                }
            });
        }

        return context;
    }

    private HttpServletRequest resolveCurrentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest();
        }
        return null;
    }

    private String writeContext(Map<String, Object> context) {
        if (context.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(context);
        } catch (JsonProcessingException ex) {
            log.warn("No se pudo serializar el contexto de auditoria operativa", ex);
            return null;
        }
    }

    private Map<String, Object> readContext(String contextJson) {
        if (contextJson == null || contextJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(contextJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException ex) {
            log.warn("No se pudo deserializar el contexto de auditoria operativa", ex);
            return Map.of();
        }
    }

    private OperationalAuditEventResponse toResponse(OperationalAuditEvent event) {
        return new OperationalAuditEventResponse(
                event.getId(),
                event.getActorUserId(),
                event.getActorUsername(),
                event.getAction(),
                event.getEntityType(),
                event.getEntityId(),
                event.getOccurredAt(),
                event.getResult(),
                readContext(event.getContextJson())
        );
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
