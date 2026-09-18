package com.schwab.assignment.report;

import com.schwab.assignment.engine.*;
import com.schwab.assignment.graph.*;
import com.schwab.assignment.ledger.*;

import org.springframework.stereotype.Service;
import java.util.*;

/** Builds a reviewer-facing run report entirely from durable workflow state and audit lineage. */
@Service
public class WorkflowReportService {
  public WorkflowRunReport report(Workflow workflow){
    Map<String,Long> actions=new TreeMap<>();workflow.audit().forEach(event->actions.merge(event.action(),1L,Long::sum));
    return new WorkflowRunReport(workflow.id(),workflow.scenario(),workflow.requirement(),workflow.status(),workflow.planVersion(),workflow.stages(),workflow.audit(),actions,workflow.retries(),workflow.rollbacks(),workflow.fallbackUsed(),workflow.meanTimeToRecoveryMs(),workflow.latencyMs());
  }
  public record WorkflowRunReport(UUID workflowId,String scenario,String requirement,WorkflowStatus status,int planVersion,Map<String,StageStatus> stages,List<AuditEvent> ledger,Map<String,Long> actionCounts,int retries,int rollbacks,boolean fallbackUsed,double meanTimeToRecoveryMs,long latencyMs) {}
}
