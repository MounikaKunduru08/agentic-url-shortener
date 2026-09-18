package com.schwab.assignment.orchestration;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WorkflowTest {
    private static final Map<String, Stage> GRAPH = Map.of("build", new Stage("build", Set.of(), false, "test-agent"));

    @Test
    void failureProducesRollbackAndSafeStop() {
        Workflow workflow = new Workflow("brownfield", "Change analytics", GRAPH);
        workflow.fail("build", "validation failed");
        assertEquals(WorkflowStatus.SAFE_STOPPED, workflow.status());
        assertEquals(StageStatus.ROLLED_BACK, workflow.stages().get("build"));
    }

    @Test
    void retryRequiresARecordedFailure() {
        Workflow workflow = new Workflow("greenfield", "Build", GRAPH);
        assertThrows(IllegalStateException.class, workflow::retry);
    }
}
