package com.garageos.modules.audit.repository;

import com.garageos.modules.audit.entity.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    List<AuditEvent> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, Long entityId);

    List<AuditEvent> findByGarageIdOrderByCreatedAtDesc(Long garageId);
}
