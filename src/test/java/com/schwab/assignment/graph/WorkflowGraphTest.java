package com.schwab.assignment.graph;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class WorkflowGraphTest {
  @Test void rejectsUnknownDependenciesAndCyclesAtConstructionTime() {
    assertThrows(IllegalArgumentException.class,()->WorkflowGraph.of(new Stage("design",Set.of("missing"),false,"agent")));
    assertThrows(IllegalArgumentException.class,()->WorkflowGraph.of(
        new Stage("design",Set.of("test"),false,"agent"),new Stage("test",Set.of("design"),false,"agent")));
  }

  @Test void createsDeterministicParallelExecutionLayers() {
    Map<String,Stage> graph=WorkflowGraph.of(
        new Stage("understand",Set.of(),false,"agent"),
        new Stage("design",Set.of("understand"),false,"agent"),
        new Stage("docs",Set.of("understand"),false,"agent"),
        new Stage("release",Set.of("design","docs"),true,"agent"));

    assertEquals(List.of(List.of("understand"),List.of("design","docs"),List.of("release")),WorkflowGraph.executionLayers(graph));
  }
}
