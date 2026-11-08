# Escalation

Escalating a ticket means flagging it to the support managers so that they step in (BR-064). Escalation is a **flag** on the ticket: it is not a status. The ticket keeps its status, priority, assigned agent and SLA.

## When to escalate

The approved rules do not set a list of escalation cases: it is the agent's decision. It is useful when the next step goes beyond your authority or needs a manager's decision, for example:

- the ticket needs to be reassigned to another agent (only a manager can reassign, BR-058, BR-059);
- you will not be able to meet the SLA target and the ticket needs to be taken in hand or redistributed;
- the case is sensitive or unusual and needs a decision you cannot take alone.

These examples are guidelines, not rules. If in doubt, talk to your manager.

## Who can escalate

| Role | Can escalate |
| --- | --- |
| Support agent | Tickets assigned to them and unassigned tickets |
| Support manager | Every ticket |
| Administrator | Every ticket |

A ticket can be escalated only once, and not when it is <span class="ui">Closed</span>. An agent cannot escalate a colleague's ticket (it is read only for them).

## How to escalate

1. Open the ticket.
2. In <span class="ui">Ticket actions</span>, click <span class="ui">Escalate to manager</span>.
3. In the <span class="ui">Escalate this ticket?</span> dialog, fill in the <span class="ui">Reason for escalation</span> (mandatory, 500 characters maximum): what is blocking, what has already been done, what you expect from the manager.
4. Click <span class="ui">Escalate</span>. The message "Ticket escalated." confirms it.

!!! tip "A useful reason"
    The manager should understand the situation without rereading the whole ticket. For details, add an internal note before escalating.

## What happens after escalation

- The ticket carries the <span class="ui">Escalated</span> badge in its header and in the queue.
- **Active support managers** receive the "Ticket escalated" notification (except the one who escalated it).
- The history records <span class="ui">Ticket escalated</span> with your reason.
- Managers and administrators see the date in the actions panel ("Escalated on …").
- The ticket is counted in the "N escalated" note of the dashboard's <span class="ui">SLA breached</span> tile while it is open, and can be found with the queue's <span class="ui">Escalated</span> checkbox.

Escalation changes nothing else: the assigned agent stays assigned and the SLA keeps running. The requester is not told about the escalation and does not see it in their history.

## What the manager does

The manager has every operational right on the ticket: they can reassign it with <span class="ui">Assign to an agent</span>, assign it to themselves, change the priority, reply, add an internal note to explain their decision, resolve or close it.

!!! note "No way to cancel an escalation"
    The application does not offer a way to remove the <span class="ui">Escalated</span> flag: it stays on the ticket, even after resolution, as a record of the escalation.
