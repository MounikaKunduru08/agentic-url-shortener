package com.schwab.assignment.persistence;

import com.schwab.assignment.graph.*;
import com.schwab.assignment.engine.*;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/** Durable snapshot of the workflow state. Detailed lineage is stored separately in WorkflowAuditRecord. */
@Entity
@Table(name = "workflow_records")
class WorkflowRecord {
  @Id private UUID id;
  @Column(nullable = false) private String scenario;
  @Column(nullable = false, length = 4000) private String requirement;
  @Enumerated(EnumType.STRING) @Column(nullable = false) private WorkflowStatus status;
  @Enumerated(EnumType.STRING) private WorkflowStatus pausedFrom;
  @Column(nullable = false) private int planVersion;
  @Column(nullable = false) private int retries;
  @Column(nullable = false) private int rollbacks;
  @Column(nullable = false) private boolean fallbackUsed;
  private String lastFailedStage;
  @Column(nullable = false) private Instant startedAt;
  private Instant failedAt;
  @Column(nullable = false) private long recoveredMillis;
  @Column(nullable = false) private int recoveries;
  @Column(nullable = false, length = 4000) private String stagesJson;
  protected WorkflowRecord() {}
  WorkflowRecord(WorkflowSnapshot snapshot, String stagesJson) {
    id=snapshot.id(); scenario=snapshot.scenario(); requirement=snapshot.requirement(); status=snapshot.status(); pausedFrom=snapshot.pausedFrom(); planVersion=snapshot.planVersion();
    retries=snapshot.retries(); rollbacks=snapshot.rollbacks(); fallbackUsed=snapshot.fallbackUsed(); lastFailedStage=snapshot.lastFailedStage();
    startedAt=snapshot.startedAt(); failedAt=snapshot.failedAt(); recoveredMillis=snapshot.recoveredMillis(); recoveries=snapshot.recoveries(); this.stagesJson=stagesJson;
  }
  UUID id(){return id;} String scenario(){return scenario;} String requirement(){return requirement;} WorkflowStatus status(){return status;} WorkflowStatus pausedFrom(){return pausedFrom;} int planVersion(){return planVersion;}
  int retries(){return retries;} int rollbacks(){return rollbacks;} boolean fallbackUsed(){return fallbackUsed;} String lastFailedStage(){return lastFailedStage;} Instant startedAt(){return startedAt;} Instant failedAt(){return failedAt;} long recoveredMillis(){return recoveredMillis;} int recoveries(){return recoveries;} String stagesJson(){return stagesJson;}
}
