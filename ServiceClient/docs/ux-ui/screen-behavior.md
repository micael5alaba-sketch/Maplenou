# Screen Behavior & States

## Global states

Initial/empty, loading, loaded, interaction, success, validation error, business-rule error, authorization error, technical error, and recovery/retry.

## Ticket lifecycle

`NEW → ASSIGNED → IN_PROGRESS → WAITING_FOR_CUSTOMER → IN_PROGRESS → RESOLVED → CLOSED`

Reopening:
`RESOLVED → Request reopening → Support decision → IN_PROGRESS (approved) / RESOLVED (rejected)`.

`CLOSED` cannot be requester-reopened.

## Create Ticket

Validate required fields and attachments. Requesters cannot directly set `CRITICAL`. Successful creation leads to ticket detail with status `NEW`.

## Communication

Customer-visible messages and support-only internal notes are structurally distinct. Message sending has validation, sending, success, and recoverable failure/retry states.

## Attachments

`Select → Client validation → Upload → Server validation → Success/Rejected`.

Approved limits: JPG/JPEG/PNG/WEBP/PDF; 10 MB/file; 5 files/message; 50 MB/ticket.

## Support actions

Action availability depends on user, role, ticket state, assignment, and authorization. Support Agents may self-assign unassigned tickets when authorized; normal cross-agent reassignment is not an Agent capability. Manager/Admin may perform permitted reassignment.

## SLA

Expose first-response and resolution states: on track, approaching, breached.

## Notifications and authorization

Notification failure must not corrupt core ticket state. Authorization failures must not leak protected data.
