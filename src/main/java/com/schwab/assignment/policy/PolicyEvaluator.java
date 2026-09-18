package com.schwab.assignment.policy;

import com.schwab.assignment.engine.Workflow;
import com.schwab.assignment.graph.*;

/** Input port for policy-as-code, rules engines, or centrally managed policy services. */
public interface PolicyEvaluator { GateResult evaluateGate(Stage stage, Workflow workflow); }
