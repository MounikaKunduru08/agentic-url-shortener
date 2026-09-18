package com.schwab.assignment.engine;

import com.schwab.assignment.graph.*;
import com.schwab.assignment.agents.*;
import com.schwab.assignment.policy.*;
import com.schwab.assignment.ledger.*;
import com.schwab.assignment.artifacts.*;

import java.time.*;
import java.util.*;

public final class Workflow {
  public static final int MAX_RETRIES = 2;
  private final UUID id; private final String scenario; private String requirement;
  private final Map<String, Stage> graph; private final Map<String, StageStatus> stages = new LinkedHashMap<>();
  private final List<AuditEvent> audit = new ArrayList<>(); private final Instant startedAt;
  private WorkflowStatus status = WorkflowStatus.RUNNING; private WorkflowStatus pausedFrom; private int planVersion = 1; private int retries; private int rollbacks; private String pendingApproval;
  private final Set<String> approvedStages = new HashSet<>(); private boolean fallbackUsed; private String lastFailedStage; private Instant failedAt; private long recoveredMillis; private int recoveries;
  public Workflow(String scenario, String requirement, Map<String, Stage> graph) { this(UUID.randomUUID(),scenario,requirement,graph,Instant.now(),WorkflowStatus.RUNNING,null,1,0,0,false,null,null,0,0,Map.of(),List.of()); graph.keySet().forEach(key -> stages.put(key, StageStatus.PENDING)); audit("workflow", "CREATED", "scenario=" + scenario); }
  public Workflow(UUID id,String scenario,String requirement,Map<String,Stage> graph,Instant startedAt,WorkflowStatus status,WorkflowStatus pausedFrom,int planVersion,int retries,int rollbacks,boolean fallbackUsed,String lastFailedStage,Instant failedAt,long recoveredMillis,int recoveries,Map<String,StageStatus> restoredStages,List<AuditEvent> restoredAudit) {
    this.id=id;this.scenario=scenario;this.requirement=requirement;this.graph=graph;this.startedAt=startedAt;this.status=status;this.pausedFrom=pausedFrom;this.planVersion=planVersion;this.retries=retries;this.rollbacks=rollbacks;this.fallbackUsed=fallbackUsed;this.lastFailedStage=lastFailedStage;this.failedAt=failedAt;this.recoveredMillis=recoveredMillis;this.recoveries=recoveries;this.stages.putAll(restoredStages);this.audit.addAll(restoredAudit);if(status==WorkflowStatus.AWAITING_APPROVAL||pausedFrom==WorkflowStatus.AWAITING_APPROVAL)this.pendingApproval=this.stages.entrySet().stream().filter(e->e.getValue()==StageStatus.AWAITING_APPROVAL).map(Map.Entry::getKey).findFirst().orElseThrow(()->new IllegalStateException("workflow approval state is inconsistent"));
  }
  public UUID id(){return id;} public String scenario(){return scenario;} public String requirement(){return requirement;} public Map<String,StageStatus> stages(){return Map.copyOf(stages);} public List<AuditEvent> audit(){return List.copyOf(audit);} public WorkflowStatus status(){return status;} public WorkflowStatus pausedFrom(){return pausedFrom;} public int planVersion(){return planVersion;} public int retries(){return retries;} public int rollbacks(){return rollbacks;} public boolean fallbackUsed(){return fallbackUsed;} public String lastFailedStage(){return lastFailedStage;} public Instant startedAt(){return startedAt;} public Instant failedAt(){return failedAt;} public long recoveredMillis(){return recoveredMillis;} public int recoveries(){return recoveries;} public long latencyMs(){return Duration.between(startedAt, Instant.now()).toMillis();} public double meanTimeToRecoveryMs(){return recoveries==0?0:(double)recoveredMillis/recoveries;}
  public WorkflowSnapshot snapshot(){return new WorkflowSnapshot(id,scenario,requirement,status,pausedFrom,planVersion,retries,rollbacks,fallbackUsed,lastFailedStage,startedAt,failedAt,recoveredMillis,recoveries,stages,audit);}
  public static Workflow restore(WorkflowSnapshot snapshot,Map<String,Stage> graph){return new Workflow(snapshot.id(),snapshot.scenario(),snapshot.requirement(),graph,snapshot.startedAt(),snapshot.status(),snapshot.pausedFrom(),snapshot.planVersion(),snapshot.retries(),snapshot.rollbacks(),snapshot.fallbackUsed(),snapshot.lastFailedStage(),snapshot.failedAt(),snapshot.recoveredMillis(),snapshot.recoveries(),snapshot.stages(),snapshot.audit());}
  void audit(String stage, String action, String detail){audit.add(AuditEvent.now(stage, action, detail, planVersion));}
  void awaitClarification(String detail){status=WorkflowStatus.AWAITING_CLARIFICATION;audit("understand","AMBIGUITY_DETECTED",detail);}
  void pause(String operator){if(status==WorkflowStatus.COMPLETED||status==WorkflowStatus.ABORTED||status==WorkflowStatus.PAUSED)throw new IllegalStateException("workflow cannot be paused in status "+status);pausedFrom=status;status=WorkflowStatus.PAUSED;audit("workflow","PAUSED","by="+operator+"; priorStatus="+pausedFrom);}
  void resume(String operator){if(status!=WorkflowStatus.PAUSED)throw new IllegalStateException("workflow is not paused");status=pausedFrom==null?WorkflowStatus.RUNNING:pausedFrom;pausedFrom=null;audit("workflow","RESUMED","by="+operator+"; restoredStatus="+status);}
  void abort(String operator,String reason){if(status==WorkflowStatus.COMPLETED||status==WorkflowStatus.ABORTED)throw new IllegalStateException("terminal workflows cannot be aborted");status=WorkflowStatus.ABORTED;audit("workflow","ABORTED","by="+operator+"; reason="+reason);}
  boolean dependenciesComplete(Stage stage) { return stage.dependsOn().stream().allMatch(d -> stages.get(d)==StageStatus.SUCCEEDED); }
  void setStage(String stage, StageStatus value, String detail) { stages.put(stage,value); audit(stage,value.name(),detail); }
  void awaitApproval(String stage) { pendingApproval=stage; status=WorkflowStatus.AWAITING_APPROVAL; setStage(stage,StageStatus.AWAITING_APPROVAL,"human approval required"); }
  void approved(String approver) { if(status != WorkflowStatus.AWAITING_APPROVAL) throw new IllegalStateException("no approval is pending"); audit(pendingApproval,"APPROVED","by="+approver); approvedStages.add(pendingApproval); stages.put(pendingApproval,StageStatus.PENDING); pendingApproval=null; status=WorkflowStatus.RUNNING; }
  boolean hasApproval(String stage) { return approvedStages.contains(stage); }
  void fail(String stage,String reason) { setStage(stage,StageStatus.FAILED,reason); rollbacks++; lastFailedStage=stage; failedAt=Instant.now(); setStage(stage,StageStatus.ROLLED_BACK,"compensating action recorded"); status=WorkflowStatus.SAFE_STOPPED; audit("workflow","SAFE_STOP","manual intervention required"); }
  boolean retry(){ if(lastFailedStage==null) throw new IllegalStateException("no failed stage is available to retry"); if(retries>=MAX_RETRIES){fallbackUsed=true;setStage(lastFailedStage,StageStatus.BLOCKED,"fallback: manual remediation required");audit(lastFailedStage,"FALLBACK","retry limit reached; routed to manual remediation");status=WorkflowStatus.SAFE_STOPPED;return false;} retries++; setStage(lastFailedStage,StageStatus.PENDING,"retry="+retries+" of "+MAX_RETRIES); audit("workflow","RETRY","bounded retry="+retries); status=WorkflowStatus.RUNNING; return true; }
  void recovered(){if(failedAt!=null){recoveredMillis+=Duration.between(failedAt,Instant.now()).toMillis();recoveries++;audit("workflow","RECOVERED","MTTR recorded after retry");failedAt=null;lastFailedStage=null;}}
  void replan(String change){replan(change,"understand");}
  void replan(String change,String changedStage){
    Set<String> invalidated=WorkflowGraph.downstreamOf(graph,changedStage);
    requirement=change; planVersion++; approvedStages.clear(); pendingApproval=null; pausedFrom=null; status=WorkflowStatus.RUNNING;
    invalidated.forEach(stage->stages.put(stage,StageStatus.PENDING));
    audit("workflow","REPLANNED","new requirement="+change+"; changed stage="+changedStage+"; invalidated="+String.join(",",invalidated)+"; prior approvals invalidated");
  }
  void completeIfDone(){if(stages.values().stream().allMatch(s->s==StageStatus.SUCCEEDED)){status=WorkflowStatus.COMPLETED;audit("workflow","COMPLETED","all dependency gates passed");}}
  Map<String,Stage> graph(){return graph;}
}
