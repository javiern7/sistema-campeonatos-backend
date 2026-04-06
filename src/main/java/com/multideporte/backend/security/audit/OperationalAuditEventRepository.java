package com.multideporte.backend.security.audit;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OperationalAuditEventRepository
        extends JpaRepository<OperationalAuditEvent, Long>, JpaSpecificationExecutor<OperationalAuditEvent> {

    @Query("""
            select count(distinct e.actorUsername)
            from OperationalAuditEvent e
            where (:from is null or e.occurredAt >= :from)
              and (:to is null or e.occurredAt <= :to)
            """)
    long countDistinctActors(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

    @Query("""
            select new com.multideporte.backend.security.audit.OperationalAuditActionCountProjection(e.action, count(e))
            from OperationalAuditEvent e
            where (:from is null or e.occurredAt >= :from)
              and (:to is null or e.occurredAt <= :to)
            group by e.action
            order by count(e) desc, e.action asc
            """)
    List<OperationalAuditActionCountProjection> findActionCounts(
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            org.springframework.data.domain.Pageable pageable
    );
}
