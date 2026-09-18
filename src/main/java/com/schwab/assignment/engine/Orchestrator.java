package com.schwab.assignment.engine;

import com.schwab.assignment.graph.*;
import com.schwab.assignment.agents.*;
import com.schwab.assignment.policy.*;
import com.schwab.assignment.artifacts.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/** Dependency-graph executor with durable snapshots, policy checks, bounded recovery, and audit lineage. */
@Service
public class Orchestrator {
  private final WorkflowStore store; private final WorkflowDefinition definition; private final WorkflowScheduler scheduler; private final EngineeringArtifacts artifacts;
  /** Supports focused unit tests without a Spring persistence context. */
  public Orchestrator() { this(new InMemoryWorkflowStore(),new WorkflowDefinition(),(stage,workflow)->new StageExecutor.ExecutionResult(true,"validation gate passed by "+stage.agent()),EngineeringArtifacts.disabled()); }
  /** Test seam: keeps focused workflow tests free of generated-artifact filesystem writes. */
  Orchestrator(StageExecutor executor, EngineeringArtifacts artifacts) { this(new InMemoryWorkflowStore(),new WorkflowDefinition(),executor,artifacts); }
  Orchestrator(WorkflowStore store,WorkflowDefinition definition,StageExecutor executor,EngineeringArtifacts artifacts){this.store=store;this.definition=definition;this.scheduler=new WorkflowScheduler(new PolicyGuard(),executor);this.artifacts=artifacts;}
  @Autowired Orchestrator(WorkflowStore store, WorkflowDefinition definition, WorkflowScheduler scheduler, EngineeringArtifacts artifacts) { this.store=store;this.definition=definition;this.scheduler=scheduler;this.artifacts=artifacts; }
  @Transactional public Workflow create(String scenario, String requirement) { Workflow flow=new Workflow(scenario,requirement,definition.graph()); artifacts.create(flow); if(artifacts.isAmbiguous(requirement)){flow.awaitClarification("clarify measurable behavior before planning");persist(flow);return flow;} advance(flow); persist(flow); return flow; }
  @Transactional public Workflow get(UUID id){return restore(store.find(id).orElseThrow(()->new NoSuchElementException("workflow not found: "+id)));}
  @Transactional public Workflow advance(Workflow flow) {Workflow result=scheduler.advance(flow);persist(result);return result;}
  @Transactional public Workflow approve(UUID id,String approver){Workflow f=get(id);f.approved(approver);return advance(f);}
  @Transactional public Workflow fail(UUID id,String stage,String reason){Workflow f=get(id); if(!f.graph().containsKey(stage))throw new IllegalArgumentException("unknown stage");f.fail(stage,reason);persist(f);return f;}
  @Transactional public Workflow retry(UUID id){Workflow f=get(id); if(f.retry()) return advance(f);persist(f);return f;}
  @Transactional public Workflow pause(UUID id,String operator){Workflow f=get(id);f.pause(operator);persist(f);return f;}
  @Transactional public Workflow resume(UUID id,String operator){Workflow f=get(id);f.resume(operator);return advance(f);}
  @Transactional public Workflow abort(UUID id,String operator,String reason){Workflow f=get(id);f.abort(operator,reason);persist(f);return f;}
  @Transactional public Workflow replan(UUID id,String changedRequirement){return replan(id,changedRequirement,"understand");}
  @Transactional public Workflow replan(UUID id,String changedRequirement,String changedStage){Workflow f=get(id);f.replan(changedRequirement,changedStage);artifacts.create(f);if(artifacts.isAmbiguous(changedRequirement)){f.awaitClarification("clarification remains incomplete");persist(f);return f;}return advance(f);}
  @Transactional public Collection<Workflow> all(){return store.findAll().stream().map(this::restore).toList();}
  /** Snapshot mutable state, then append only audit events not already stored for this run. */
  private void persist(Workflow workflow){store.save(workflow.snapshot());}
  private Workflow restore(WorkflowSnapshot snapshot){return Workflow.restore(snapshot,definition.graph());}
}
