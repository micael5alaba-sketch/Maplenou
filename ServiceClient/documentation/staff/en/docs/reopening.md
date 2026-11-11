# Reopening

When a problem comes back after resolution, the requester can request reopening of their ticket. Support decides: the requester can never reopen a ticket themselves (BR-049, BR-050).

## Reopening rules

| Rule | Reference |
| --- | --- |
| Only the requester can request reopening, and only for **their own** <span class="ui">Resolved</span> ticket. | BR-048, DEC-007 |
| A <span class="ui">Closed</span> ticket cannot be reopened: the requester must create a new ticket. | BR-052, BR-053 |
| Only one request can be pending at a time on a ticket. | Application behaviour |
| An authorized support agent or support manager reviews and decides. | BR-050 |
| An approved reopening returns the ticket to <span class="ui">In progress</span>. | BR-051 |
| Requests and decisions are recorded in the history. | BR-054 |
| The original SLA history is kept and a new resolution period starts. | BR-055 |

## Who can decide

- the **agent assigned** to the ticket;
- any **support manager**;
- any **administrator**.

An agent who is not assigned to the ticket sees the request in the history, but not the decision buttons.

## Being told about a request

When a requester requests reopening:

- the assigned agent receives the "Reopening request" notification; if the ticket is unassigned, the active support managers receive it;
- the history records <span class="ui">Reopening requested</span> with the requester's reason;
- the ticket stays <span class="ui">Resolved</span> until the request is handled.

## Approving or refusing a request

At the top of the ticket screen, the "Reopening request of *date*" box shows the requester's reason in quotation marks.

1. Read the reason, the conversation and the history.
2. Write the <span class="ui">Decision note (visible to the requester)</span>: explain your decision in a few clear sentences. It is optional but recommended, especially when you refuse.
3. Click:
    - <span class="ui">Reopen (In progress)</span> to approve: the ticket goes back to <span class="ui">In progress</span> (message "Ticket reopened.");
    - <span class="ui">Refuse</span> to refuse: the ticket stays <span class="ui">Resolved</span> (message "Request refused.").

!!! warning "The decision note is visible"
    Unlike internal notes, the decision note is shown to the requester in their ticket's history. Do not put any internal information in it.

## After an approved reopening

- The ticket is <span class="ui">In progress</span>, with the same assigned agent.
- A **new SLA resolution period** starts at the time of approval, based on the current priority; the first-response deadline does not change (see [SLA and calendar](sla.md)).
- The history records <span class="ui">Reopening approved</span> then <span class="ui">Status changed</span>.
- The requester is notified that their request was approved.

Then resume work as usual: reply to the requester, and mark the ticket resolved again once the problem is fixed.

## After a refusal

- The ticket stays <span class="ui">Resolved</span>.
- The history records <span class="ui">Reopening refused</span> with your note.
- The requester is notified that their request was refused. They can make a new request as long as the ticket is <span class="ui">Resolved</span>.

## Closing a ticket with a pending request

While a reopening request is pending, the ticket cannot be closed: the <span class="ui">Close ticket</span> button is not offered, and an attempt returns "A reopening request is pending: handle it before closing the ticket." Decide on the request first.

## Request about a closed ticket

If a requester contacts you about a <span class="ui">Closed</span> ticket, explain that they need to create a new ticket from their area, ideally quoting the old ticket number in the description.
