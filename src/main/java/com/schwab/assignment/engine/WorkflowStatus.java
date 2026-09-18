package com.schwab.assignment.engine;

import com.schwab.assignment.graph.*;
import com.schwab.assignment.agents.*;
import com.schwab.assignment.policy.*;
import com.schwab.assignment.ledger.*;
import com.schwab.assignment.artifacts.*;
public enum WorkflowStatus { RUNNING, PAUSED, AWAITING_CLARIFICATION, AWAITING_APPROVAL, SAFE_STOPPED, ABORTED, COMPLETED }
