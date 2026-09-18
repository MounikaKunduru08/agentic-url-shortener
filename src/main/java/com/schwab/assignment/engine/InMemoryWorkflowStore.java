package com.schwab.assignment.engine;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Test adapter for the workflow persistence port; deliberately not a Spring bean. */
final class InMemoryWorkflowStore implements WorkflowStore {
  private final Map<UUID,WorkflowSnapshot> snapshots=new ConcurrentHashMap<>();
  public void save(WorkflowSnapshot snapshot){snapshots.put(snapshot.id(),snapshot);}
  public Optional<WorkflowSnapshot> find(UUID workflowId){return Optional.ofNullable(snapshots.get(workflowId));}
  public Collection<WorkflowSnapshot> findAll(){return List.copyOf(snapshots.values());}
}
