# Good practices

These recommendations help provide a consistent and safe service. They complement the application's rules without replacing them: when a rule applies (confidentiality of internal notes, Critical priority, attachments), it is repeated with its reference.

## Writing a reply to the requester

- **Greet and name the problem**: restate in one sentence what you understood from the request.
- **Be clear and concrete**: short sentences, one idea per paragraph, numbered steps when the requester has to act.
- **Say what will happen next**: what you have done, what you will do, what you expect from them.
- **Stay polite and neutral**, even with an unhappy requester. No internal jargon (technical statuses, error codes, tool names).
- **Proofread before sending**: a message sent in the conversation cannot be edited and stays in the history.

## Choosing the language of the reply

The application is available in French and English. Reply in the language the requester used in their ticket; if they write in English, reply in English. Avoid mixing both languages in the same message.

In-app notifications are shown in the language chosen by the user; push notifications (browser and mobile apps) are currently sent in French.

## Using internal notes

Internal notes (<span class="ui">Internal notes</span> tab) are **never** visible to the requester (BR-016). Use them to:

- record a check ("Payment checked: status confirmed with the provider");
- prepare a handover to a colleague or a manager;
- ask the team for advice without exposing the discussion to the requester;
- explain a decision (priority change, refused reopening) for whoever picks up the ticket.

Before sending, always check the active tab: <span class="ui">Conversation</span> = visible to the requester, <span class="ui">Internal notes</span> = support only.

## Asking the requester for information

1. First send a message in the <span class="ui">Conversation</span> that lists **all** the information you need at once (order number, photo of the parcel, screenshot…).
2. Then click <span class="ui">Request information</span>: the ticket moves to <span class="ui">Waiting for customer</span> and the requester is notified.
3. As soon as they reply, the ticket automatically moves back to <span class="ui">In progress</span> and you are notified.

The SLA clock keeps running while you wait (see [SLA and calendar](sla.md)): without an answer, follow up with a new message.

## Protecting personal and financial data

!!! danger "Never ask for"
    **Never** ask for a password, a verification code or a full bank-card number, neither in the conversation nor through any other channel. Support never needs them. If a requester sends one, do not copy it and invite them to change their password or contact their bank.

- Payment, payout, commission and seller-balance information shown in the ticket context is reserved for support: do not copy it into the conversation, except what directly concerns the requester and what they need.
- Do not mention another requester's data in a ticket.
- For attachments, ask only for what is useful. Accepted formats: JPG, JPEG, PNG, WEBP, PDF; 10 MB per file, 5 files per message, 50 MB per ticket (DEC-003).

## Using the Critical priority wisely

The <span class="ui">Critical</span> priority imposes the shortest targets (1 hour for the first response, 4 hours for resolution, counted 24/7) and every change to Critical is audited (BR-038).

- Only support can set it (BR-036): a requester's <span class="ui">Reported as urgent</span> badge is a signal to assess, not an obligation (BR-037).
- The approved rules do not define a list of "critical" cases. Keep it for situations that cannot wait for the target of a <span class="ui">High</span> priority, and if in doubt, ask your manager.
- Always fill in the <span class="ui">Reason</span> in the confirmation dialog (for example "suspected fraud"): it is kept in the history and helps your colleagues.
- Setting an already old ticket to Critical can make it overdue immediately: reply straight away.

## Resolving and closing properly

- Before <span class="ui">Mark resolved</span>, send a message that summarises the solution and tells the requester they can request reopening if the problem persists.
- Only close a resolved ticket when the requester no longer needs it: after closure, they will have to create a new ticket (BR-053).
- Use the <span class="ui">Comment (history, optional)</span> field of the confirmation dialogs to keep a record of why you made the change.
