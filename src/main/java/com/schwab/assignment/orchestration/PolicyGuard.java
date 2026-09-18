package com.schwab.assignment.orchestration;

import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Small explicit policy layer; production implementations can replace these rules with approved policy-as-code.
 */
@Component
class PolicyGuard {
    PolicyDecision evaluate(Stage stage, Workflow workflow) {
        String text = workflow.requirement().toLowerCase(Locale.ROOT);
        if (text.contains("bypass security") || text.contains("disable audit") || text.contains("skip validation"))
            return PolicyDecision.denied("SECURITY", "requirement requests a prohibited control bypass");
        if ((text.contains("pii") || text.contains("personal data")) && !text.contains("retention"))
            return PolicyDecision.denied("COMPLIANCE", "personal-data work requires a retention requirement");
        if ((text.contains("production") || text.contains("release")) && !text.contains("change ticket"))
            return PolicyDecision.denied("CHANGE_CONTROL", "production work requires a change ticket reference");
        if (stage.id().equals("release") && !stage.approvalRequired())
            return PolicyDecision.denied("CHANGE_CONTROL", "release must require approval");
        return PolicyDecision.permitted();
    }

    record PolicyDecision(boolean allowed, String domain, String reason) {
        static PolicyDecision permitted() {
            return new PolicyDecision(true, "POLICY", "all applicable controls passed");
        }

        static PolicyDecision denied(String domain, String reason) {
            return new PolicyDecision(false, domain, reason);
        }
    }
}
