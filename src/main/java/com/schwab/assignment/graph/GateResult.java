package com.schwab.assignment.graph;

import java.util.Map;

/** Exhaustive engine-level gate outcomes, modelled after a governed DAG executor. */
public sealed interface GateResult permits GateResult.Pass,GateResult.Fail,GateResult.Block,GateResult.Escalate {
  String reason(); Map<String,String> details();
  record Pass(String reason,Map<String,String> details) implements GateResult {}
  record Fail(String reason,Map<String,String> details) implements GateResult {}
  record Block(String reason,Map<String,String> details) implements GateResult {}
  record Escalate(String reason,Map<String,String> details) implements GateResult {}
  static Pass pass(String reason){return new Pass(reason,Map.of());}
  static Block block(String domain,String reason){return new Block(reason,Map.of("domain",domain));}
}
