package com.schwab.assignment.orchestration;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface WorkflowRecordRepository extends JpaRepository<WorkflowRecord, UUID> {
}
