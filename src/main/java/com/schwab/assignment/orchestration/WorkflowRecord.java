package com.schwab.assignment.orchestration;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Durable snapshot of the workflow state. Detailed lineage is stored separately in WorkflowAuditRecord.
 */
@Entity
@Table(name = "workflow_records")
class WorkflowRecord {
    @Id
    private UUID id;
    @Column(nullable = false)
    private String scenario;
    @Column(nullable = false, length = 4000)
    private String requirement;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkflowStatus status;
    @Column(nullable = false)
    private int planVersion;
    @Column(nullable = false)
    private int retries;
    @Column(nullable = false)
    private int rollbacks;
    @Column(nullable = false)
    private boolean fallbackUsed;
    private String lastFailedStage;
    @Column(nullable = false)
    private Instant startedAt;
    private Instant failedAt;
    @Column(nullable = false)
    private long recoveredMillis;
    @Column(nullable = false)
    private int recoveries;
    @Column(nullable = false, length = 4000)
    private String stagesJson;

    protected WorkflowRecord() {
    }

    WorkflowRecord(Workflow workflow, String stagesJson) {
        id = workflow.id();
        scenario = workflow.scenario();
        requirement = workflow.requirement();
        status = workflow.status();
        planVersion = workflow.planVersion();
        retries = workflow.retries();
        rollbacks = workflow.rollbacks();
        fallbackUsed = workflow.fallbackUsed();
        lastFailedStage = workflow.lastFailedStage();
        startedAt = workflow.startedAt();
        failedAt = workflow.failedAt();
        recoveredMillis = workflow.recoveredMillis();
        recoveries = workflow.recoveries();
        this.stagesJson = stagesJson;
    }

    UUID id() {
        return id;
    }

    String scenario() {
        return scenario;
    }

    String requirement() {
        return requirement;
    }

    WorkflowStatus status() {
        return status;
    }

    int planVersion() {
        return planVersion;
    }

    int retries() {
        return retries;
    }

    int rollbacks() {
        return rollbacks;
    }

    boolean fallbackUsed() {
        return fallbackUsed;
    }

    String lastFailedStage() {
        return lastFailedStage;
    }

    Instant startedAt() {
        return startedAt;
    }

    Instant failedAt() {
        return failedAt;
    }

    long recoveredMillis() {
        return recoveredMillis;
    }

    int recoveries() {
        return recoveries;
    }

    String stagesJson() {
        return stagesJson;
    }
}
