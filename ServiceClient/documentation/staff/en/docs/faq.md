# FAQ

The questions the support team asks most often. Click a question to show the answer.

## Tickets and rights

??? question "Why can't I reply to this ticket?"
    Three possible cases:

    - the ticket is **assigned to a colleague**: the <span class="ui">Read only</span> box is shown; only the assigned agent or a manager can change it;
    - the ticket is **unassigned**: first click <span class="ui">Take over</span>, then reply;
    - the ticket is **closed**: no action is possible ("Ticket closed: no actions available.").

    See [Roles and access](roles-and-access.md).

??? question "A colleague is away: can I take over their ticket?"
    No, an agent cannot reassign another agent's ticket (BR-058). Ask a support manager: they can assign it to you with <span class="ui">Assign to an agent</span>. If the ticket is stuck, you can also ask the assigned agent or a manager to escalate it.

??? question "Can the requester see an internal note?"
    No. Internal notes and the files attached to an internal note are never visible to the requester (BR-016). On the other hand, everything you write in the <span class="ui">Conversation</span> tab and the decision note of a reopening are visible to them.

??? question "Can I edit or delete a message I have already sent?"
    No. The application does not let you edit or delete a message or an internal note, and the history cannot be edited (BR-018). If you made a mistake, send a new message that corrects it.

## Priority and SLA

??? question "The requester says it is urgent: should I set the ticket to Critical?"
    Not automatically. The <span class="ui">Reported as urgent</span> badge is information from the requester; it never sets the <span class="ui">Critical</span> priority (BR-037). Assess the situation and choose the appropriate priority. See [Good practices](good-practices.md).

??? question "Does the SLA stop while the ticket is Waiting for customer?"
    No. The clock keeps running during <span class="ui">Waiting for customer</span>: pausing has not been approved. Ask for all the information you need at once and follow up if necessary. See [SLA and calendar](sla.md).

??? question "Why did the ticket become “Overdue” right after a priority change?"
    A priority change recalculates the deadlines from the ticket's creation (first response, if nobody has replied yet) and from the start of the resolution period. If the new, shorter target has already elapsed, the state immediately becomes <span class="ui">Overdue</span>. The history shows the <span class="ui">SLA recalculated</span> entry.

## Resolution, reopening, escalation

??? question "I can't find the Close ticket button on a resolved ticket."
    A reopening request is probably pending: the "Reopening request" box appears at the top of the ticket. Approve or refuse it first; closing becomes possible again if you refuse. See [Reopening](reopening.md).

??? question "The requester wants to reopen a closed ticket."
    That is not possible (BR-052). Invite them to create a new ticket from their area, quoting the old ticket number.

??? question "How do I cancel an escalation?"
    The application does not let you remove the <span class="ui">Escalated</span> flag. If the escalation is no longer needed, say so in an internal note and tell your manager. See [Escalation](escalation.md).

## Screen and incidents

??? question "The Order panel shows “Service temporarily unavailable.”"
    The Maplenou service that provides this information is not responding for the moment. The rest of the ticket works normally: keep handling the ticket and reload the page later to display the context.

??? question "The message “The ticket was changed by someone else. Reload and try again.” is shown."
    A colleague changed the same ticket at the same time as you. Reload the page, check what changed (<span class="ui">History</span> tab) and repeat your action if it is still needed.
