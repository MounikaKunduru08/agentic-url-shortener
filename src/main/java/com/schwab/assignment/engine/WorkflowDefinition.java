package com.schwab.assignment.engine;

import com.schwab.assignment.graph.*;
import org.springframework.stereotype.Component;
import java.util.*;

/** Stable workflow definition owned by the engine, independent of transport and storage. */
@Component
public class WorkflowDefinition {
  private final Map<String,Stage> graph=WorkflowGraph.of(
      new Stage("understand",Set.of(),false,"requirements-agent"),
      new Stage("design",Set.of("understand"),false,"architecture-agent"),
      new Stage("implement",Set.of("design"),false,"implementation-agent"),
      new Stage("test",Set.of("design"),false,"verification-agent"),
      new Stage("docs",Set.of("understand"),false,"documentation-agent"),
      new Stage("release",Set.of("implement","test","docs"),true,"release-agent"));
  public Map<String,Stage> graph(){return graph;}
}
