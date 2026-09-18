package com.schwab.assignment.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface WorkflowAuditRepository extends JpaRepository<WorkflowAuditRecord, Long> { List<WorkflowAuditRecord> findByWorkflowIdOrderByOccurredAtAsc(UUID workflowId); }
