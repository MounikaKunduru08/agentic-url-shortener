package com.schwab.assignment.orchestration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {
    private final Orchestrator orchestrator;

    WorkflowController(Orchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    record CreateRequest(@NotBlank String scenario, @NotBlank String requirement) {
    }

    record ApprovalRequest(@NotBlank String approver) {
    }

    record FailureRequest(@NotBlank String stage, @NotBlank String reason) {
    }

    record ReplanRequest(@NotBlank String changedRequirement) {
    }

    record WorkflowView(UUID id, String scenario, String requirement, WorkflowStatus status, int planVersion,
                        int retries, int maxRetries, int rollbacks, boolean fallbackUsed, double meanTimeToRecoveryMs,
                        long latencyMs, Map<String, StageStatus> stages, List<AuditEvent> audit) {
        static WorkflowView of(Workflow w) {
            return new WorkflowView(w.id(), w.scenario(), w.requirement(), w.status(), w.planVersion(), w.retries(), Workflow.MAX_RETRIES, w.rollbacks(), w.fallbackUsed(), w.meanTimeToRecoveryMs(), w.latencyMs(), w.stages(), w.audit());
        }
    }

    @PostMapping
    WorkflowView create(@Valid @RequestBody CreateRequest r) {
        return WorkflowView.of(orchestrator.create(r.scenario(), r.requirement()));
    }

    @GetMapping("/{id}")
    WorkflowView get(@PathVariable UUID id) {
        return WorkflowView.of(orchestrator.get(id));
    }

    @PostMapping("/{id}/approve")
    WorkflowView approve(@PathVariable UUID id, @Valid @RequestBody ApprovalRequest r) {
        return WorkflowView.of(orchestrator.approve(id, r.approver()));
    }

    @PostMapping("/{id}/failure")
    WorkflowView failure(@PathVariable UUID id, @Valid @RequestBody FailureRequest r) {
        return WorkflowView.of(orchestrator.fail(id, r.stage(), r.reason()));
    }

    @PostMapping("/{id}/retry")
    WorkflowView retry(@PathVariable UUID id) {
        return WorkflowView.of(orchestrator.retry(id));
    }

    @PostMapping("/{id}/replan")
    WorkflowView replan(@PathVariable UUID id, @Valid @RequestBody ReplanRequest r) {
        return WorkflowView.of(orchestrator.replan(id, r.changedRequirement()));
    }
}
