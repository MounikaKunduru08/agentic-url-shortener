package com.schwab.assignment.orchestration;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface WorkflowAuditRepository extends JpaRepository<WorkflowAuditRecord, Long> {
    List<WorkflowAuditRecord> findByWorkflowIdOrderByOccurredAtAsc(UUID workflowId);

    void deleteByWorkflowId(UUID workflowId);
}
