package com.schwab.assignment.engine;

import java.util.*;

/** Output port for durable workflow snapshots and their append-only audit lineage. */
public interface WorkflowStore {
  void save(WorkflowSnapshot snapshot);
  Optional<WorkflowSnapshot> find(UUID workflowId);
  Collection<WorkflowSnapshot> findAll();
}
