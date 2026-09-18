package com.schwab.assignment.orchestration;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

interface WorkflowAuditRepository extends JpaRepository<WorkflowAuditRecord, Long> {
    List<WorkflowAuditRecord> findByWorkflowIdOrderByOccurredAtAsc(UUID workflowId);

    void deleteByWorkflowId(UUID workflowId);
}
