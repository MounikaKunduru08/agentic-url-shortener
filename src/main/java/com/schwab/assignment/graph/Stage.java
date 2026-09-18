package com.schwab.assignment.graph;
import java.util.Set;
public record Stage(String id, Set<String> dependsOn, boolean approvalRequired, String agent) {
  public Stage {
    if(id==null||id.isBlank())throw new IllegalArgumentException("stage id must be non-blank");
    if(agent==null||agent.isBlank())throw new IllegalArgumentException("stage agent must be non-blank");
    dependsOn=dependsOn==null?Set.of():Set.copyOf(dependsOn);
    if(dependsOn.contains(id))throw new IllegalArgumentException("stage cannot depend on itself: "+id);
  }
}
