package com.schwab.assignment.orchestration;

import java.util.Set;

public record Stage(String id, Set<String> dependsOn, boolean approvalRequired, String agent) {
}
