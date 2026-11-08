# Maplenou Customer Service — Business Rules

**ID:** CS-01-BR | **Status:** Approved | **Version:** 1.0.0

## Core ticket rules

- **BR-001:** Every ticket has a unique technical ID.
- **BR-002:** Every ticket has a unique human-readable ticket number; numbers are never reused.
- **BR-003:** Only authenticated customers, sellers, and delivery drivers may create tickets.
- **BR-004:** Requester identity comes from the authenticated identity, not an arbitrary body field.
- **BR-005:** A ticket has exactly one primary category in the MVP.
- **BR-006:** Allowed categories are Order, Payment, Delivery, Seller, Account, Return/Refund, Other. *(Amended by DEC-CS11-06: administrators may add categories, give them an English name and deactivate them; a category is never deleted and Other always stays active.)*
- **BR-007:** Allowed priorities are Low, Medium, High, Critical.
- **BR-008:** Priority changes are recorded in history.
- **BR-009:** A new ticket starts in `NEW`.
- **BR-010:** Only valid lifecycle transitions are allowed; `NEW → CLOSED` is invalid.
- **BR-011:** Reopening must be authorized and audited.
- **BR-012:** Only authorized support users may assign tickets.
- **BR-013:** Assignment targets an active support agent and is audited.
- **BR-014:** Messages belong to one ticket and require an authenticated author.
- **BR-015:** Participants may send messages only when authorized for that ticket.
- **BR-016:** Internal notes are visible only to authorized support personnel.
- **BR-017:** Significant mutations create history entries identifying actor and timestamp.
- **BR-018:** Normal support operations cannot edit history.
- **BR-019:** Contextual order/payment/delivery/seller/refund references require authorization validation.
- **BR-020:** Notification delivery is an asynchronous side effect and must not invalidate a successful core ticket mutation.
- **BR-021:** Attachments require authentication, authorization, file-type, size, storage, and security controls.
- **BR-022:** Failed ticket operations must not leave partially persisted core ticket state.

## Approved SLA rules — DEC-001

- **BR-029:** Each ticket receives SLA targets based on its current priority.
- **BR-030:** The system tracks the first-response target.
- **BR-031:** The system tracks the resolution target.
- **BR-032:** SLA time is calculated using Monday–Friday, 08:00–18:00 business hours, excluding officially recognized public holidays. *(Amended by DEC-CS10-16: 24/7 by default, `SLA_MODE=business-hours` keeps this rule; and by DEC-CS11-03: the resolution clock is paused while the ticket waits for the requester, and the due date moves by the paused time.)*
  *Amended 2026-10-29 by DEC-CS10-16: SLA time is counted 24 hours a day, 7 days a week, weekends and public holidays included (`SLA_MODE=always`, default); the business-hours rule remains available as `SLA_MODE=business-hours`.*
- **BR-033:** SLA starts when the ticket is created.
- **BR-034:** A priority change recalculates the applicable SLA while preserving the previous priority and change history.
- **BR-035:** A ticket whose applicable response or resolution target has passed without the required action is considered SLA-breached.

## Approved critical-priority rules — DEC-002

- **BR-036:** Only Support Agents, Support Managers, and Administrators may assign or change a ticket to `CRITICAL`.
- **BR-037:** A requester may report urgency, but requester-provided urgency does not automatically assign `CRITICAL`.
- **BR-038:** Every assignment or change to `CRITICAL` is audited.
- **BR-039:** `CRITICAL` tickets use the Critical SLA.

## Approved attachment rules — DEC-003

- **BR-040:** Allowed attachment types are JPG, JPEG, PNG, WEBP, and PDF.
- **BR-041:** An individual attachment must not exceed 10 MB.
- **BR-042:** A message must not contain more than 5 attachments.
- **BR-043:** Total attachment size for a ticket must not exceed 50 MB.
- **BR-044:** A user must be authorized for the ticket before attaching a file.
- **BR-045:** Attachment type and size are validated before acceptance.
- **BR-046:** Attachments are accessible only to users authorized for the associated ticket.
- **BR-047:** Attachment failure must not corrupt the ticket or existing messages.

## Approved reopening rules — DEC-004 / DEC-007

- **BR-048:** A requester may request reopening of their own `RESOLVED` ticket.
- **BR-049:** A requester cannot directly transition a ticket from `RESOLVED` to an active status.
- **BR-050:** An authorized Support Agent or Support Manager reviews and approves reopening.
- **BR-051:** An approved reopening returns the ticket to `IN_PROGRESS`, unless another valid active status is explicitly selected by authorized support personnel.
- **BR-052:** A requester cannot reopen a `CLOSED` ticket.
- **BR-053:** Assistance concerning a closed ticket requires a new ticket.
- **BR-054:** Reopening requests and decisions are audited.
- **BR-055:** Reopening preserves original SLA history and creates a new applicable active-resolution SLA period.

## Approved Support Agent permission rules — DEC-005

- **BR-056:** A Support Agent has operational authority over tickets they are assigned to or otherwise authorized to handle.
- **BR-057:** A Support Agent may assign an unassigned ticket to themselves.
- **BR-058:** A Support Agent may not normally reassign a ticket owned by another Support Agent.
- **BR-059:** A Support Manager may reassign tickets between Support Agents.
- **BR-060:** A Support Agent may perform valid lifecycle transitions within their authority.
- **BR-061:** A Support Agent may change category and priority, including `CRITICAL`, subject to authorization.
- **BR-062:** A Support Agent may resolve a ticket when the issue has been addressed.
- **BR-063:** A Support Agent may close a ticket according to the approved closure policy.
- **BR-064:** A Support Agent may escalate a ticket to a Support Manager.
- **BR-065:** A Support Agent may not manage users, roles, or system configuration unless separately authorized.
- **BR-066:** Significant Support Agent actions are audited.

## Approved Support Agent visibility rules — DEC-006

- **BR-067:** Support Agents may view tickets available to the support team, including unassigned tickets.
- **BR-068:** A Support Agent may take ownership of an unassigned ticket according to assignment rules.
- **BR-069:** A Support Agent may view tickets assigned to another Support Agent for legitimate support-operation purposes.
- **BR-070:** Viewing another agent's ticket does not automatically grant modification, resolution, closure, or reassignment rights.
- **BR-071:** Each protected ticket operation is independently authorized according to role, ticket relationship, and operation.
- **BR-072:** Sensitive ticket information is exposed only to users authorized to access it.

## General authorization rules

- **BR-073:** Protected ticket operations require an authenticated user with an appropriate role.
- **BR-074:** Role membership alone does not grant unrestricted access to every ticket.
- **BR-075:** Authorization is evaluated before protected mutations are performed.
- **BR-076:** Actions not explicitly authorized are denied by default.

## Rules added in CS-11 (DEC-CS11-02 … 08)

- **BR-077:** Statuses, transitions, priorities, SLA targets and permissions are configurable by administrators, within the lifecycle and safety rules (one system status per kind; custom statuses are "in progress" or "on hold"; requesters only ever reach their own tickets and never internal notes; CRITICAL is never chosen by a requester).
- **BR-078:** A resolved ticket that is not reopened is closed automatically after the configured delay (7 days by default).
- **BR-079:** The requester may rate a resolved or closed ticket once, from 1 to 5, with an optional comment.
- **BR-080:** Actions on the Maplenou platform (return request, delivery release, message to the courier) are requested by the agent handling the ticket (or a supervisor) and decided by Maplenou with its own rules; the outcome is recorded on the ticket.
- **BR-081:** AI suggestions and reply drafts are advisory: nothing is applied or sent without an agent, and only the subject, the description and public messages are sent to the assistant.
