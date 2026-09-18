package com.schwab.assignment.api;

import com.schwab.assignment.engine.*;
import com.schwab.assignment.graph.*;
import com.schwab.assignment.ledger.*;
import com.schwab.assignment.report.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/workflows")
public class WorkflowController {
  private final Orchestrator orchestrator; private final WorkflowReportService reports; WorkflowController(Orchestrator orchestrator,WorkflowReportService reports){this.orchestrator=orchestrator;this.reports=reports;}
  record CreateRequest(@NotBlank String scenario,@NotBlank String requirement){} record ApprovalRequest(@NotBlank String approver){} record FailureRequest(@NotBlank String stage,@NotBlank String reason){} record ReplanRequest(@NotBlank String changedRequirement,String changedStage){} record InterventionRequest(@NotBlank String operator,String reason){}
  record WorkflowView(UUID id,String scenario,String requirement,WorkflowStatus status,int planVersion,int retries,int maxRetries,int rollbacks,boolean fallbackUsed,double meanTimeToRecoveryMs,long latencyMs,Map<String,StageStatus> stages,List<AuditEvent> audit) { static WorkflowView of(Workflow w){return new WorkflowView(w.id(),w.scenario(),w.requirement(),w.status(),w.planVersion(),w.retries(),Workflow.MAX_RETRIES,w.rollbacks(),w.fallbackUsed(),w.meanTimeToRecoveryMs(),w.latencyMs(),w.stages(),w.audit());}}
  @PostMapping WorkflowView create(@Valid @RequestBody CreateRequest r){return WorkflowView.of(orchestrator.create(r.scenario(),r.requirement()));}
  @GetMapping("/{id}") WorkflowView get(@PathVariable UUID id){return WorkflowView.of(orchestrator.get(id));}
  @GetMapping("/{id}/report") WorkflowReportService.WorkflowRunReport report(@PathVariable UUID id){return reports.report(orchestrator.get(id));}
  @PostMapping("/{id}/approve") WorkflowView approve(@PathVariable UUID id,@Valid @RequestBody ApprovalRequest r){return WorkflowView.of(orchestrator.approve(id,r.approver()));}
  @PostMapping("/{id}/failure") WorkflowView failure(@PathVariable UUID id,@Valid @RequestBody FailureRequest r){return WorkflowView.of(orchestrator.fail(id,r.stage(),r.reason()));}
  @PostMapping("/{id}/retry") WorkflowView retry(@PathVariable UUID id){return WorkflowView.of(orchestrator.retry(id));}
  @PostMapping("/{id}/pause") WorkflowView pause(@PathVariable UUID id,@Valid @RequestBody InterventionRequest r){return WorkflowView.of(orchestrator.pause(id,r.operator()));}
  @PostMapping("/{id}/resume") WorkflowView resume(@PathVariable UUID id,@Valid @RequestBody InterventionRequest r){return WorkflowView.of(orchestrator.resume(id,r.operator()));}
  @PostMapping("/{id}/abort") WorkflowView abort(@PathVariable UUID id,@Valid @RequestBody InterventionRequest r){return WorkflowView.of(orchestrator.abort(id,r.operator(),r.reason()==null?"operator requested":r.reason()));}
  @PostMapping("/{id}/replan") WorkflowView replan(@PathVariable UUID id,@Valid @RequestBody ReplanRequest r){return WorkflowView.of(orchestrator.replan(id,r.changedRequirement(),r.changedStage()==null||r.changedStage().isBlank()?"understand":r.changedStage()));}
}
