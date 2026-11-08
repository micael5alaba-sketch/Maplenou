# Statuses and transitions

A ticket follows a controlled lifecycle: only some status changes are allowed (BR-010). The ticket screen only offers the buttons that are valid from the current status; any other request is refused and the status stays unchanged.

## The six statuses

| Status | Meaning | Open? |
| --- | --- | :---: |
| <span class="ui">New</span> | Ticket created by the requester; nobody has taken it yet. Every ticket starts here (BR-009). | Yes |
| <span class="ui">Assigned</span> | An agent is responsible for the ticket but has not started working on it yet. | Yes |
| <span class="ui">In progress</span> | Support is actively working on the request. | Yes |
| <span class="ui">Waiting for customer</span> | Support is waiting for information or an action from the requester. | Yes |
| <span class="ui">Resolved</span> | The problem has been addressed. The requester can still request reopening. | No |
| <span class="ui">Closed</span> | Ticket finalised. No further action possible; a new request needs a new ticket. | No |

"Open" tickets are the ones counted by the <span class="ui">Open tickets</span> tile and the <span class="ui">Open (all)</span> filter.

## Diagram of the allowed transitions

```mermaid
stateDiagram-v2
    state "New" as NEW
    state "Assigned" as ASSIGNED
    state "In progress" as IN_PROGRESS
    state "Waiting for customer" as WAITING
    state "Resolved" as RESOLVED
    state "Closed" as CLOSED

    [*] --> NEW : created by the requester
    NEW --> ASSIGNED : assignment (automatic)
    ASSIGNED --> IN_PROGRESS : Start work / first reply
    ASSIGNED --> WAITING : Request information
    IN_PROGRESS --> WAITING : Request information
    WAITING --> IN_PROGRESS : Resume work / requester reply
    IN_PROGRESS --> RESOLVED : Mark resolved
    WAITING --> RESOLVED : Mark resolved
    RESOLVED --> CLOSED : Close ticket
    RESOLVED --> IN_PROGRESS : reopening approved
    CLOSED --> [*]
```

## Transition table and who can make each change

| From | To | How | Who |
| --- | --- | --- | --- |
| <span class="ui">New</span> | <span class="ui">Assigned</span> | Automatic on the first assignment (<span class="ui">Take over</span> or <span class="ui">Assign to an agent</span>). There is no status button for this step. | Agent (on an unassigned ticket), manager, administrator (assignment to an agent) |
| <span class="ui">Assigned</span> | <span class="ui">In progress</span> | <span class="ui">Start work</span>, or automatically on support's first visible reply | Assigned agent, manager, administrator |
| <span class="ui">Assigned</span> or <span class="ui">In progress</span> | <span class="ui">Waiting for customer</span> | <span class="ui">Request information</span> | Assigned agent, manager, administrator |
| <span class="ui">Waiting for customer</span> | <span class="ui">In progress</span> | <span class="ui">Resume work</span>, or automatically when the requester replies | Assigned agent, manager, administrator; the requester indirectly through their reply |
| <span class="ui">In progress</span> or <span class="ui">Waiting for customer</span> | <span class="ui">Resolved</span> | <span class="ui">Mark resolved</span> (with confirmation) | Assigned agent, manager, administrator |
| <span class="ui">Resolved</span> | <span class="ui">Closed</span> | <span class="ui">Close ticket</span> (with confirmation), only if no reopening request is pending | Assigned agent, manager, administrator |
| <span class="ui">Resolved</span> | <span class="ui">In progress</span> | Only by approving a reopening request from the requester (<span class="ui">Reopen (In progress)</span>) | Assigned agent, manager, administrator |

Every status change is recorded in the history with its author, date and, if provided, your comment (BR-017). The requester is notified of status changes on their ticket.

## Forbidden transitions

- <span class="ui">New</span> → <span class="ui">Closed</span> is explicitly forbidden (BR-010): a ticket must be handled and resolved before it is closed.
- <span class="ui">Resolved</span> → an active status without an approved reopening request is forbidden, including for support (BR-049). The requester cannot reopen directly.
- <span class="ui">Closed</span> is final: no status change is possible, neither by support nor by the requester (BR-052). A new request needs a new ticket (BR-053).
- An agent cannot change the status of a ticket assigned to a colleague, nor of an unassigned ticket until they have taken it.

When an attempt is refused, the application shows "This status change is not allowed."

## Automatic changes

| Event | Change |
| --- | --- |
| First assignment of a <span class="ui">New</span> ticket | <span class="ui">New</span> → <span class="ui">Assigned</span> |
| Support's first visible reply on an <span class="ui">Assigned</span> ticket | <span class="ui">Assigned</span> → <span class="ui">In progress</span> |
| Requester's reply on a <span class="ui">Waiting for customer</span> ticket | <span class="ui">Waiting for customer</span> → <span class="ui">In progress</span> |
| Reopening approved | <span class="ui">Resolved</span> → <span class="ui">In progress</span> |

An internal note never changes the status. A <span class="ui">Resolved</span> ticket that the requester does not reopen is **closed automatically after 7 days** (DEC-CS11-04); the history shows "Closed automatically". Administrators can add statuses and change the allowed moves (see [Administration](administration.md#statuses-and-priorities)).

!!! note "Target status of a reopening"
    Rule BR-051 states that an approved reopening returns the ticket to <span class="ui">In progress</span>, unless support explicitly selects another valid active status. The screen currently only offers <span class="ui">Reopen (In progress)</span>.
