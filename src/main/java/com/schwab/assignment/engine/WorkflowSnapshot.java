package com.schwab.assignment.engine;

import com.schwab.assignment.graph.StageStatus;
import com.schwab.assignment.ledger.AuditEvent;
import java.time.Instant;
import java.util.*;

/** Persistence-neutral representation of a workflow. Storage adapters never receive mutable domain state. */
public record WorkflowSnapshot(UUID id,String scenario,String requirement,WorkflowStatus status,WorkflowStatus pausedFrom,int planVersion,int retries,int rollbacks,boolean fallbackUsed,String lastFailedStage,Instant startedAt,Instant failedAt,long recoveredMillis,int recoveries,Map<String,StageStatus> stages,List<AuditEvent> audit) {
  public WorkflowSnapshot { stages=Map.copyOf(stages); audit=List.copyOf(audit); }
}
