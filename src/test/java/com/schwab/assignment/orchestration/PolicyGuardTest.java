package com.schwab.assignment.orchestration;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PolicyGuardTest {
  private final PolicyGuard guard=new PolicyGuard();
  private final Stage stage=new Stage("understand",Set.of(),false,"requirements-agent");
  @Test void blocksSecurityBypass() { assertFalse(guard.evaluate(stage,new Workflow("x","bypass security",Map.of("understand",stage))).allowed()); }
  @Test void blocksPersonalDataWithoutRetention() { assertEquals("COMPLIANCE",guard.evaluate(stage,new Workflow("x","Store PII",Map.of("understand",stage))).domain()); }
  @Test void allowsCompliantRequirement() { assertTrue(guard.evaluate(stage,new Workflow("x","Store PII with retention policy",Map.of("understand",stage))).allowed()); }
}
