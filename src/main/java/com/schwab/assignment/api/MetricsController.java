package com.schwab.assignment.api;

import com.schwab.assignment.engine.*;
import com.schwab.assignment.graph.*;
import com.schwab.assignment.ledger.*;
import com.schwab.assignment.report.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/metrics")
public class MetricsController { private final Orchestrator o; MetricsController(Orchestrator o){this.o=o;}
  record ReliabilityMetrics(long workflows,long completed,double successRate,long retries,long rollbacks,long fallbacks,double meanTimeToRecoveryMs,double meanLatencyMs){}
  @GetMapping ReliabilityMetrics get(){var all=o.all(); long completed=all.stream().filter(w->w.status()==WorkflowStatus.COMPLETED).count();return new ReliabilityMetrics(all.size(),completed,all.isEmpty()?0:(double)completed/all.size(),all.stream().mapToLong(Workflow::retries).sum(),all.stream().mapToLong(Workflow::rollbacks).sum(),all.stream().filter(Workflow::fallbackUsed).count(),all.stream().mapToDouble(Workflow::meanTimeToRecoveryMs).average().orElse(0),all.stream().mapToLong(Workflow::latencyMs).average().orElse(0));}}
