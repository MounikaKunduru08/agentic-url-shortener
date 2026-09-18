package com.schwab.assignment.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schwab.assignment.engine.*;
import com.schwab.assignment.graph.StageStatus;
import com.schwab.assignment.ledger.AuditEvent;
import org.springframework.stereotype.Repository;
import java.util.*;

/** H2/JPA adapter for the engine's persistence port. It is the only layer aware of JPA records. */
@Repository
public class JpaWorkflowStore implements WorkflowStore {
  private final WorkflowRecordRepository records; private final WorkflowAuditRepository audits; private final ObjectMapper mapper;
  public JpaWorkflowStore(WorkflowRecordRepository records,WorkflowAuditRepository audits,ObjectMapper mapper){this.records=records;this.audits=audits;this.mapper=mapper;}
  public void save(WorkflowSnapshot snapshot){try{
    records.save(new WorkflowRecord(snapshot,mapper.writeValueAsString(snapshot.stages())));
    Set<AuditEvent> stored=new HashSet<>(audits.findByWorkflowIdOrderByOccurredAtAsc(snapshot.id()).stream().map(WorkflowAuditRecord::event).toList());
    audits.saveAll(snapshot.audit().stream().filter(event->!stored.contains(event)).map(event->new WorkflowAuditRecord(snapshot.id(),event)).toList());
  }catch(JsonProcessingException exception){throw new IllegalStateException("could not persist workflow state",exception);}}
  public Optional<WorkflowSnapshot> find(UUID workflowId){return records.findById(workflowId).map(this::snapshot);}
  public Collection<WorkflowSnapshot> findAll(){return records.findAll().stream().map(this::snapshot).toList();}
  private WorkflowSnapshot snapshot(WorkflowRecord record){try{
    Map<String,StageStatus> stages=mapper.readValue(record.stagesJson(),new TypeReference<>(){});
    List<AuditEvent> audit=audits.findByWorkflowIdOrderByOccurredAtAsc(record.id()).stream().map(WorkflowAuditRecord::event).toList();
    return new WorkflowSnapshot(record.id(),record.scenario(),record.requirement(),record.status(),record.pausedFrom(),record.planVersion(),record.retries(),record.rollbacks(),record.fallbackUsed(),record.lastFailedStage(),record.startedAt(),record.failedAt(),record.recoveredMillis(),record.recoveries(),stages,audit);
  }catch(JsonProcessingException exception){throw new IllegalStateException("could not restore workflow state",exception);}}
}
