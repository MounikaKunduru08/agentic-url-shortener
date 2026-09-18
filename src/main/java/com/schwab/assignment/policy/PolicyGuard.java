package com.schwab.assignment.policy;

import com.schwab.assignment.graph.*;
import com.schwab.assignment.engine.*;

import org.springframework.stereotype.Component;
import java.util.Locale;

/** Small explicit policy layer; production implementations can replace these rules with approved policy-as-code. */
@Component
public class PolicyGuard implements PolicyEvaluator {
  public GateResult evaluateGate(Stage stage,Workflow workflow){
    PolicyDecision decision=evaluate(stage,workflow);
    return decision.allowed()?GateResult.pass(decision.reason()):GateResult.block(decision.domain(),decision.reason());
  }
  public PolicyDecision evaluate(Stage stage, Workflow workflow) {
    String text=workflow.requirement().toLowerCase(Locale.ROOT);
    if (text.contains("bypass security") || text.contains("disable audit") || text.contains("skip validation")) return PolicyDecision.denied("SECURITY", "requirement requests a prohibited control bypass");
    if ((text.contains("pii") || text.contains("personal data")) && !text.contains("retention")) return PolicyDecision.denied("COMPLIANCE", "personal-data work requires a retention requirement");
    if ((text.contains("production") || text.contains("release")) && !text.contains("change ticket")) return PolicyDecision.denied("CHANGE_CONTROL", "production work requires a change ticket reference");
    if (stage.id().equals("release") && !stage.approvalRequired()) return PolicyDecision.denied("CHANGE_CONTROL", "release must require approval");
    return PolicyDecision.permitted();
  }
  public record PolicyDecision(boolean allowed, String domain, String reason) { public static PolicyDecision permitted(){return new PolicyDecision(true,"POLICY","all applicable controls passed");} public static PolicyDecision denied(String domain,String reason){return new PolicyDecision(false,domain,reason);} }
}
