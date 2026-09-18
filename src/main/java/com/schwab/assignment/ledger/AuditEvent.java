package com.schwab.assignment.ledger;
import java.time.Instant;
public record AuditEvent(Instant at, String stage, String action, String detail, int planVersion) {
  public static AuditEvent now(String stage, String action, String detail, int version) { return new AuditEvent(Instant.now(), stage, action, detail, version); }
}
