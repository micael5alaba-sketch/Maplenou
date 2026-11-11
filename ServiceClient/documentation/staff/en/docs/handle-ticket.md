# Handle a ticket

The ticket screen brings together everything you need to handle it without switching tools: the requester and the Maplenou context (order, payment, delivery…) on the left, the exchanges in the middle, the actions and the SLA on the right. Only the buttons you are allowed to use on this ticket are shown.

## Opening a ticket and reading the header

![A ticket's workspace](images/ticket-workspace.jpg)

*Marks on the screenshot:*

1. The tabs <span class="ui">Conversation</span>, <span class="ui">Internal notes</span>, <span class="ui">History</span> and <span class="ui">Attachments</span>.
2. <span class="ui">Insert a canned reply</span>: a text ready to adapt.
3. <span class="ui">Draft by the assistant</span>: a proposed reply to review (never sent without you).
4. <span class="ui">Ticket actions</span>: status, priority, category, escalation.
5. <span class="ui">Actions on Maplenou</span>: return request, delivery release, message to the courier.
6. The Maplenou context: <span class="ui">Order</span>, then payment, delivery and seller.

Open a ticket from the <span class="ui">Queue</span>, <span class="ui">My tickets</span>, the <span class="ui">Dashboard</span> or a notification. The header shows:

- the breadcrumb <span class="ui">Tickets</span> / ticket number (format `TKT-year-number`, never reused);
- the status badge, the priority badge and, when relevant, the <span class="ui">Escalated</span> badge;
- the subject, then the category, the creation date and the requester's name;
- the <span class="ui">Reported as urgent</span> badge if the requester ticked urgency when creating the ticket. It is information to help you set the priority: it **never** sets the ticket to <span class="ui">Critical</span> automatically (BR-037).

If the ticket is assigned to a colleague, the <span class="ui">Read only</span> box reminds you that only the assigned agent or a manager can change it.

## Checking the requester and the Maplenou context

The left column is reserved for support.

| Panel | Information |
| --- | --- |
| <span class="ui">Requester details</span> | Name, e-mail, <span class="ui">Profile</span> (Customer, Seller or Delivery driver), <span class="ui">Phone</span>, <span class="ui">Customer since</span>, number of the requester's <span class="ui">Tickets</span>. |
| <span class="ui">Order</span> | <span class="ui">No.</span>, <span class="ui">Date</span>, <span class="ui">Amount</span>, <span class="ui">Status</span>, <span class="ui">Seller</span>. |
| <span class="ui">Payment</span> | <span class="ui">No.</span>, <span class="ui">Status</span>, <span class="ui">Provider</span> (FedaPay, Stripe), <span class="ui">Commission</span>, <span class="ui">Payout</span> (status and amount). |
| <span class="ui">Delivery</span> | <span class="ui">No.</span>, <span class="ui">Driver</span>, <span class="ui">Status</span>, <span class="ui">Destination</span>, <span class="ui">Time slot</span>, <span class="ui">Last update</span>. |
| <span class="ui">Seller</span> | <span class="ui">Shop</span>, <span class="ui">Account</span> (account status), <span class="ui">Balance</span>. |
| <span class="ui">Refund</span> | <span class="ui">No.</span>, <span class="ui">Status</span>, <span class="ui">Amount</span>. |

!!! note "How the commission works (since November 2026)"
    The Maplenou commission is **included in the price the buyer sees**: 5 % of the seller's base price
    (Standard category) or 3 % (High-price category), rounded to the whole FCFA per unit, delivery excluded. The
    buyer pays nothing extra and the seller receives their base price: the commission is never taken from the
    payout. Orders placed before this change keep the former model (25 % / 22.5 % taken from the seller).

    - Example (Standard): base price 10,000 FCFA + commission 500 FCFA = **10,500 FCFA** shown, delivery extra.
    - **Monthly seller fee**: 500 FCFA (Standard) or 2,000 FCFA (High price). The first 3 completed sales are free;
      from the 4th, **one** fee per month, only for a month with at least one completed sale. It is deducted from
      the available balance **before** the payout (8,000 FCFA available − 500 FCFA = 7,500 FCFA paid); if the
      balance is too low, the rest stays due for the next payout. The same fee is never taken twice.
    - A **cancelled or refunded** order does not count as a sale; the commission included in its price is refunded
      with it.
    - The seller chooses the category at sign-up; Maplenou may ask for a correction if it does not match the
      products. An approved change applies from the 1st of the next month, to new orders only.
    - The seller is notified at the 4th sale (fee starts), at each deduction and at each refund that includes a
      commission. In the app, the seller sees the fee summary (category, amount, month, status), the history of
      deductions and the estimated net amount before a withdrawal.

Panels appear only when the ticket is linked to a reference. When creating the ticket, the requester can link an order or a delivery they are part of (a customer: an order they bought; a seller: an order they sold; a driver: a delivery assigned to them). The payment, delivery, seller and refund references are then derived automatically from the order.

!!! info "Service temporarily unavailable"
    Each panel loads separately. If a Maplenou service does not respond, the panel shows "Service temporarily unavailable."; the rest of the ticket remains usable. "Reference not found." means the linked reference could not be found.

Actions in other modules (refund, re-schedule a delivery, contact the driver from the application) are not available from the ticket: no approved rule defines them yet.

## Taking or assigning the ticket

The <span class="ui">Ticket actions</span> panel shows <span class="ui">Assigned to:</span> followed by the agent's name or <span class="ui">Unassigned</span>.

- **Agent**: on an unassigned ticket, click <span class="ui">Take over</span>. The ticket is assigned to you and moves from <span class="ui">New</span> to <span class="ui">Assigned</span>.
- **Manager**: <span class="ui">Take over</span> (unassigned ticket) or <span class="ui">Assign to me</span> (ticket already assigned); the <span class="ui">Assign to an agent</span> list → <span class="ui">Choose an agent…</span> assigns the ticket to an active agent or manager. Each option shows the agent's workload, in the form "Name (3 open)".
- **Administrator**: can assign to an agent with <span class="ui">Assign to an agent</span>, but cannot assign the ticket to themselves.

Every assignment or reassignment is recorded in the history (BR-013) and the agent receiving the ticket is notified.

## Adjusting priority and category

In <span class="ui">Ticket actions</span>:

- <span class="ui">Priority</span>: choose <span class="ui">Low</span>, <span class="ui">Medium</span>, <span class="ui">High</span> or <span class="ui">Critical</span>. The change is immediate, except for <span class="ui">Critical</span>, which opens the <span class="ui">Set priority to Critical?</span> dialog: enter a <span class="ui">Reason</span> (the field suggests "E.g. suspected fraud"), then <span class="ui">Confirm Critical</span>. Every priority change recalculates the SLA (see [SLA and calendar](sla.md)).
- <span class="ui">Category</span>: choose an active category; the message "Category changed." confirms it.

Both changes are recorded in the history with the old and new values (BR-008).

## Replying in the conversation

The <span class="ui">Conversation</span> tab contains the messages visible to the requester, including their initial description. Write in the <span class="ui">Reply to the customer…</span> field, then click <span class="ui">Send</span>.

- Your first visible reply records the **first response** for the SLA; if the ticket is <span class="ui">Assigned</span>, it automatically moves to <span class="ui">In progress</span>.
- The requester is notified of every reply.
- If sending fails, a message offers <span class="ui">Retry</span>: the message is never saved twice.
- A message can contain up to 5,000 characters.

To reply to or change the status of an unassigned ticket, take it first.

## Writing an internal note

The <span class="ui">Internal notes</span> tab is for exchanges between support staff: investigation, handover, asking for advice. Write in the <span class="ui">Note visible to support only…</span> field, then click <span class="ui">Add note</span>.

!!! danger "Never visible to the requester"
    Internal notes and their attachments are **never** visible to the requester (BR-016), neither in the application nor in notifications. Conversely, everything you write in the <span class="ui">Conversation</span> tab is visible to them: check the active tab before sending.

An internal note does not count as a first response for the SLA.

## Attaching files

Below the input field (conversation or note), <span class="ui">+ Attachment</span> adds files to the message:

- accepted formats: JPG, JPEG, PNG, WEBP, PDF;
- 10 MB maximum per file, 5 files maximum per message, 50 MB maximum in total per ticket (DEC-003).

The type is checked against the file's real content: a renamed file is refused. The <span class="ui">Attachments</span> tab lists every file of the ticket with its size, author and date; files that come from an internal note carry the <span class="ui">Internal note</span> label. Click a file to download it.

## Changing the status

The <span class="ui">Change status</span> area offers only the changes allowed from the current status:

| Button | Effect |
| --- | --- |
| <span class="ui">Start work</span> | <span class="ui">Assigned</span> → <span class="ui">In progress</span> |
| <span class="ui">Request information</span> | → <span class="ui">Waiting for customer</span>; the requester is notified that support needs more information |
| <span class="ui">Resume work</span> | <span class="ui">Waiting for customer</span> → <span class="ui">In progress</span> |
| <span class="ui">Mark resolved</span> | → <span class="ui">Resolved</span>, after confirmation in <span class="ui">Mark the ticket as resolved?</span> |
| <span class="ui">Close ticket</span> | <span class="ui">Resolved</span> → <span class="ui">Closed</span>, after confirmation in <span class="ui">Close the ticket?</span> |

The confirmation dialogs offer a <span class="ui">Comment (history, optional)</span>, saved in the history. Transition details are in [Statuses and transitions](statuses-and-transitions.md).

!!! warning "Closing is final"
    A <span class="ui">Closed</span> ticket can no longer be reopened by the requester: they will have to create a new ticket (BR-052, BR-053). Only close a resolved ticket the requester no longer needs.

## Escalating

<span class="ui">Escalate to manager</span> flags the ticket to the support managers. The reason is mandatory. See [Escalation](escalation.md).

## Checking the history

The <span class="ui">History</span> tab traces every significant event with its author and date: <span class="ui">Ticket created</span>, <span class="ui">Status changed</span>, <span class="ui">Priority changed</span>, <span class="ui">Category changed</span>, <span class="ui">Assigned</span>, <span class="ui">Reassigned</span>, <span class="ui">Message sent</span>, <span class="ui">Internal note added</span>, <span class="ui">Attachment added</span>, <span class="ui">Ticket escalated</span>, <span class="ui">Reopening requested</span>, <span class="ui">Reopening approved</span>, <span class="ui">Reopening refused</span>, <span class="ui">SLA recalculated</span>. Changes show the old and new values.

The history can be neither edited nor deleted (BR-018). The requester sees a reduced version: creation, changes of status, priority and category, attachments of visible messages and the reopening steps (with the reason and the decision note). They do not see assignments, internal notes, escalations, SLA recalculations or your comments and reasons.

## Reading the SLA panel

Below the actions, the <span class="ui">SLA</span> panel shows the state of the <span class="ui">First response</span> and of the <span class="ui">Resolution</span>, each with its deadline, and the reply or resolution date once reached. After a reopening, an expandable line lists the resolution periods. See [SLA and calendar](sla.md).

## Using a canned reply or an assistant draft

Below the reply box of the <span class="ui">Conversation</span> tab:

- the <span class="ui">Canned replies</span> list inserts a text prepared by managers (see [Administration](administration.md#canned-replies-and-notification-texts));
- the <span class="ui">✨ Draft by the assistant</span> button (when AI help is on) writes a reply from the user guide, in your language, signed with your name. The titles of the sections used are shown next to it.

In both cases the text is only **inserted**: read it, adapt it to the requester, then click <span class="ui">Send</span>. The assistant only receives the subject, description and public messages of the ticket — never the name, phone, e-mail or payment data.

## Category and priority suggestion

When AI help is on, the assistant reads each new ticket and proposes a category and a priority in the <span class="ui">✨ Assistant suggestion</span> card, with a confidence and a short reason. Click <span class="ui">Apply</span> to change the category and priority (written in the history), or <span class="ui">Dismiss</span>. Nothing is applied without you; the requester never sees the suggestion.

## Acting on Maplenou from the ticket

The <span class="ui">Actions on Maplenou</span> card appears when the ticket is assigned to you (or for a manager) and is linked to an order or a delivery:

| Button | What Maplenou does |
| --- | --- |
| <span class="ui">Request a return / refund</span> | opens a return request on behalf of the buyer (reason of at least 10 characters, product condition, shop concerned when the order has several); the usual rules apply: 30 days after delivery, one request per shop; the seller then decides |
| <span class="ui">Release the delivery</span> | takes the delivery away from the current courier so that another one can take it; not possible once the courier is on the way |
| <span class="ui">Message the courier</span> | sends your message to the courier's support conversation in the Maplenou app, with the ticket number and your name |

Maplenou may refuse a request: its message is shown (for example "the 30-day return window after delivery has passed"). Accepted or refused, every request is listed under <span class="ui">Actions sent</span> and in the ticket history.

## If an action fails

- "The ticket was changed by someone else. Reload and try again.": a colleague changed the ticket at the same time as you. Reload the page and repeat the action if it is still useful.
- "This status change is not allowed.": the requested status change is not allowed from the current status.
- "The ticket must be assigned to an active support agent.": the chosen agent is deactivated or cannot be assigned.
