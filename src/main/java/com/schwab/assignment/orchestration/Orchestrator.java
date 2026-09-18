package com.schwab.assignment.orchestration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Dependency-graph executor with durable snapshots, policy checks, bounded recovery, and audit lineage.
 */
@Service
public class Orchestrator {
    private final Map<UUID, Workflow> memory = new ConcurrentHashMap<>();
    private final WorkflowRecordRepository records;
    private final WorkflowAuditRepository audits;
    private final ObjectMapper mapper;
    private final PolicyGuard policies;
    private final StageExecutor executor;
    private final EngineeringArtifacts artifacts;
    private final ExecutorService stagePool = Executors.newFixedThreadPool(3, runnable -> {
        Thread thread = new Thread(runnable, "workflow-stage");
        thread.setDaemon(true);
        return thread;
    });
    private static final Map<String, Stage> GRAPH = Map.of(
            "understand", new Stage("understand", Set.of(), false, "requirements-agent"),
            "design", new Stage("design", Set.of("understand"), false, "architecture-agent"),
            "implement", new Stage("implement", Set.of("design"), false, "implementation-agent"),
            "test", new Stage("test", Set.of("design"), false, "verification-agent"),
            "docs", new Stage("docs", Set.of("understand"), false, "documentation-agent"),
            "release", new Stage("release", Set.of("implement", "test", "docs"), true, "release-agent"));

    /**
     * Supports focused unit tests without a Spring persistence context.
     */
    public Orchestrator() {
        records = null;
        audits = null;
        mapper = new ObjectMapper();
        policies = new PolicyGuard();
        executor = (stage, workflow) -> new StageExecutor.ExecutionResult(true, "validation gate passed by " + stage.agent());
        artifacts = new EngineeringArtifacts();
    }

    @Autowired
    Orchestrator(WorkflowRecordRepository records, WorkflowAuditRepository audits, ObjectMapper mapper, PolicyGuard policies, StageExecutor executor, EngineeringArtifacts artifacts) {
        this.records = records;
        this.audits = audits;
        this.mapper = mapper;
        this.policies = policies;
        this.executor = executor;
        this.artifacts = artifacts;
    }

    @Transactional
    public Workflow create(String scenario, String requirement) {
        Workflow flow = new Workflow(scenario, requirement, GRAPH);
        memory.put(flow.id(), flow);
        artifacts.create(flow);
        if (artifacts.isAmbiguous(requirement)) {
            flow.awaitClarification("clarify measurable behavior before planning");
            persist(flow);
            return flow;
        }
        advance(flow);
        persist(flow);
        return flow;
    }

    @Transactional
    public Workflow get(UUID id) {
        if (records == null) return inMemory(id);
        return restore(records.findById(id).orElseThrow(() -> new NoSuchElementException("workflow not found: " + id)));
    }

    /**
     * Starts every currently-ready independent stage together, then joins the batch before
     * evaluating the next dependency gate. Workflow state itself is only mutated on this
     * synchronized orchestrator thread; worker threads execute the stage boundary only.
     */
    @Transactional
    public synchronized Workflow advance(Workflow flow) {
        if (flow.status() == WorkflowStatus.SAFE_STOPPED || flow.status() == WorkflowStatus.AWAITING_APPROVAL || flow.status() == WorkflowStatus.AWAITING_CLARIFICATION)
            return flow;
        boolean progressed;
        do {
            progressed = false;
            List<Stage> runnable = new ArrayList<>();
            for (Stage s : flow.graph().values())
                if (flow.stages().get(s.id()) == StageStatus.PENDING && flow.dependenciesComplete(s)) {
                    PolicyGuard.PolicyDecision decision = policies.evaluate(s, flow);
                    if (!decision.allowed()) {
                        flow.audit(s.id(), "POLICY_DENIED", decision.domain() + ": " + decision.reason());
                        flow.fail(s.id(), "policy denied: " + decision.reason());
                        persist(flow);
                        return flow;
                    }
                    flow.audit(s.id(), "POLICY_PASSED", decision.domain());
                    if (s.approvalRequired() && !flow.hasApproval(s.id())) {
                        flow.awaitApproval(s.id());
                        persist(flow);
                        return flow;
                    }
                    flow.setStage(s.id(), StageStatus.RUNNING, "agent=" + s.agent());
                    runnable.add(s);
                }
            Map<Stage, Future<StageExecutor.ExecutionResult>> executions = new LinkedHashMap<>();
            for (Stage stage : runnable) executions.put(stage, stagePool.submit(() -> executor.execute(stage, flow)));
            for (Map.Entry<Stage, Future<StageExecutor.ExecutionResult>> entry : executions.entrySet()) {
                Stage stage = entry.getKey();
                StageExecutor.ExecutionResult result;
                try {
                    result = entry.getValue().get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    flow.fail(stage.id(), "stage execution interrupted");
                    persist(flow);
                    return flow;
                } catch (ExecutionException e) {
                    flow.fail(stage.id(), "stage executor error: " + e.getCause().getMessage());
                    persist(flow);
                    return flow;
                }
                if (!result.succeeded()) {
                    flow.fail(stage.id(), result.detail());
                    persist(flow);
                    return flow;
                }
                flow.setStage(stage.id(), StageStatus.SUCCEEDED, result.detail());
                if (stage.id().equals(flow.lastFailedStage())) flow.recovered();
                progressed = true;
            }
        } while (progressed);
        flow.completeIfDone();
        persist(flow);
        return flow;
    }

    @Transactional
    public Workflow approve(UUID id, String approver) {
        Workflow f = get(id);
        f.approved(approver);
        return advance(f);
    }

    @Transactional
    public Workflow fail(UUID id, String stage, String reason) {
        Workflow f = get(id);
        if (!f.graph().containsKey(stage)) throw new IllegalArgumentException("unknown stage");
        f.fail(stage, reason);
        persist(f);
        return f;
    }

    @Transactional
    public Workflow retry(UUID id) {
        Workflow f = get(id);
        if (f.retry()) return advance(f);
        persist(f);
        return f;
    }

    @Transactional
    public Workflow replan(UUID id, String changedRequirement) {
        Workflow f = get(id);
        f.replan(changedRequirement);
        artifacts.create(f);
        if (artifacts.isAmbiguous(changedRequirement)) {
            f.awaitClarification("clarification remains incomplete");
            persist(f);
            return f;
        }
        return advance(f);
    }

    @Transactional
    public Collection<Workflow> all() {
        if (records == null) return List.copyOf(memory.values());
        return records.findAll().stream().map(this::restore).toList();
    }

    private Workflow inMemory(UUID id) {
        Workflow f = memory.get(id);
        if (f == null) throw new NoSuchElementException("workflow not found: " + id);
        return f;
    }

    private void persist(Workflow w) {
        if (records == null) return;
        try {
            records.save(new WorkflowRecord(w, mapper.writeValueAsString(w.stages())));
            audits.deleteByWorkflowId(w.id());
            audits.saveAll(w.audit().stream().map(e -> new WorkflowAuditRecord(w.id(), e)).toList());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("could not persist workflow state", e);
        }
    }

    private Workflow restore(WorkflowRecord r) {
        try {
            Map<String, StageStatus> stages = mapper.readValue(r.stagesJson(), new TypeReference<>() {
            });
            List<AuditEvent> events = audits.findByWorkflowIdOrderByOccurredAtAsc(r.id()).stream().map(WorkflowAuditRecord::event).toList();
            return new Workflow(r.id(), r.scenario(), r.requirement(), GRAPH, r.startedAt(), r.status(), r.planVersion(), r.retries(), r.rollbacks(), r.fallbackUsed(), r.lastFailedStage(), r.failedAt(), r.recoveredMillis(), r.recoveries(), stages, events);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("could not restore workflow state", e);
        }
    }

    @PreDestroy
    void stopStagePool() {
        stagePool.shutdown();
    }
}
