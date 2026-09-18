package com.schwab.assignment.engine;

import com.schwab.assignment.graph.*;
import com.schwab.assignment.agents.*;
import com.schwab.assignment.policy.*;
import com.schwab.assignment.ledger.*;
import com.schwab.assignment.artifacts.*;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.*;

/**
 * Generic workflow engine: schedules ready DAG nodes in parallel batches and applies
 * policy, approval, failure, and recovery semantics without knowing any HTTP details.
 */
@Component
final class WorkflowScheduler {
  private final PolicyEvaluator policies; private final StageExecutor executor;
  private final ExecutorService stagePool=Executors.newFixedThreadPool(3,runnable->{Thread thread=new Thread(runnable,"workflow-stage");thread.setDaemon(true);return thread;});
  WorkflowScheduler(PolicyEvaluator policies,StageExecutor executor){this.policies=policies;this.executor=executor;}
  synchronized Workflow advance(Workflow flow){
    if(flow.status()==WorkflowStatus.SAFE_STOPPED||flow.status()==WorkflowStatus.ABORTED||flow.status()==WorkflowStatus.PAUSED||flow.status()==WorkflowStatus.AWAITING_APPROVAL||flow.status()==WorkflowStatus.AWAITING_CLARIFICATION)return flow;
    boolean progressed;
    do {
      progressed=false; List<Stage> runnable=new ArrayList<>();
      for(Stage stage:flow.graph().values())if(flow.stages().get(stage.id())==StageStatus.PENDING&&flow.dependenciesComplete(stage)){
        GateResult gate=policies.evaluateGate(stage,flow);
        switch(gate){
          case GateResult.Pass pass -> flow.audit(stage.id(),"POLICY_PASSED",pass.reason());
          case GateResult.Block block -> {flow.audit(stage.id(),"POLICY_DENIED",block.details().get("domain")+": "+block.reason());flow.fail(stage.id(),"policy denied: "+block.reason());return flow;}
          case GateResult.Fail fail -> {flow.fail(stage.id(),"gate failed: "+fail.reason());return flow;}
          case GateResult.Escalate escalate -> {flow.awaitClarification(escalate.reason());return flow;}
        }
        if(stage.approvalRequired()&&!flow.hasApproval(stage.id())){flow.awaitApproval(stage.id());return flow;}
        flow.setStage(stage.id(),StageStatus.RUNNING,"agent="+stage.agent()); runnable.add(stage);
      }
      Map<Stage,Future<StageExecutor.ExecutionResult>> executions=new LinkedHashMap<>();
      for(Stage stage:runnable)executions.put(stage,stagePool.submit(()->executor.execute(stage,flow)));
      for(Map.Entry<Stage,Future<StageExecutor.ExecutionResult>> entry:executions.entrySet()){
        Stage stage=entry.getKey(); StageExecutor.ExecutionResult result;
        try {result=entry.getValue().get();}
        catch(InterruptedException exception){Thread.currentThread().interrupt();flow.fail(stage.id(),"stage execution interrupted");return flow;}
        catch(ExecutionException exception){flow.fail(stage.id(),"stage executor error: "+exception.getCause().getMessage());return flow;}
        if(!result.succeeded()){flow.fail(stage.id(),result.detail());return flow;}
        flow.setStage(stage.id(),StageStatus.SUCCEEDED,result.detail());if(stage.id().equals(flow.lastFailedStage()))flow.recovered();progressed=true;
      }
    }while(progressed);
    flow.completeIfDone();return flow;
  }
  @PreDestroy void stop(){stagePool.shutdown();}
}
