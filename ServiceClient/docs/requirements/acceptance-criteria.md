# CS-01 Acceptance Criteria

**Status:** Approved | **Version:** 1.0.0

| ID | Given | When | Then |
| --- | --- | --- | --- |
| AC-001 | authenticated eligible requester | valid ticket submitted | unique ticket is created with `NEW`, unique number, and applicable SLA targets |
| AC-002 | unauthenticated user | ticket creation attempted | request is rejected |
| AC-003 | requester | unrelated ticket requested | access is denied |
| AC-004 | valid ticket and authorized actor | valid status transition requested | transition succeeds and is audited |
| AC-005 | valid ticket | invalid transition requested | transition is rejected and status is unchanged |
| AC-006 | authorized support user | active agent selected | assignment succeeds and is audited |
| AC-007 | external requester | ticket containing internal note requested | internal note is not exposed |
| AC-008 | authorized participant | valid message sent | message is persisted on the correct ticket |
| AC-009 | authorized agent | linked order ticket opened | permitted order context is available |
| AC-010 | significant mutation | operation succeeds | actor, action, and timestamp are recorded |
| AC-011 | valid mutation | notification delivery fails | core ticket state remains consistent |
| AC-012 | authorized user | closure conditions satisfied | ticket becomes `CLOSED` and closure is audited |
| AC-013 | ticket priority is Low/Medium/High/Critical | ticket is created | first-response and resolution targets are calculated from creation time using the approved business calendar |
| AC-014 | requester reports an urgent problem | requester submits ticket | urgency does not automatically assign `CRITICAL` |
| AC-015 | requester attempts to set `CRITICAL` | priority change submitted | operation is rejected |
| AC-016 | authorized support user | `CRITICAL` priority assigned | priority changes to `CRITICAL` and the change is audited |
| AC-017 | authorized participant | unsupported or oversized attachment submitted | attachment is rejected and existing ticket state remains consistent |
| AC-018 | authorized participant | valid attachment within limits submitted | attachment is accepted and is accessible only to authorized users |
| AC-019 | requester owns a `RESOLVED` ticket | reopening requested | reopening request is recorded for support review |
| AC-020 | authorized support user | reopening approved | ticket returns to `IN_PROGRESS`, reopening is audited, and new applicable resolution SLA is established |
| AC-021 | requester owns a `CLOSED` ticket | reopening attempted | direct reopening is rejected |
| AC-022 | Support Agent | unassigned ticket selected | agent may take ownership according to assignment rules |
| AC-023 | Support Agent | another agent's ticket viewed | ticket can be viewed, but unauthorized modification/reassignment is denied |
| AC-024 | Support Agent | user/role/system configuration change attempted | operation is denied unless separately authorized |
| AC-025 | authorized support user | SLA target passes without required action | ticket is identifiable as SLA-breached |
