package com.schwab.assignment.persistence;

import com.schwab.assignment.ledger.AuditEvent;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workflow_audit_records")
public class WorkflowAuditRecord {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @Column(nullable = false) private UUID workflowId;
  @Column(nullable = false) private Instant occurredAt;
  @Column(nullable = false) private String stage;
  @Column(nullable = false) private String action;
  @Column(nullable = false, length = 4000) private String detail;
  @Column(nullable = false) private int planVersion;
  protected WorkflowAuditRecord() {}
  public WorkflowAuditRecord(UUID workflowId, AuditEvent event) { this.workflowId=workflowId; occurredAt=event.at(); stage=event.stage(); action=event.action(); detail=event.detail(); planVersion=event.planVersion(); }
  public AuditEvent event(){return new AuditEvent(occurredAt,stage,action,detail,planVersion);}
}
