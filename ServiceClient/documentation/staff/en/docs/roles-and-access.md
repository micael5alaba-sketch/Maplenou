# Roles and access

Access to a ticket depends on three things: your role, your relationship with the ticket (assigned to you, unassigned, assigned to a colleague) and the action requested. Having a support role does not give every right on every ticket: each action is checked separately, and anything not explicitly allowed is denied (BR-071, BR-074, BR-076).

## The three support roles

| Role | Label in the application | Mission |
| --- | --- | --- |
| Support agent | <span class="ui">Support agent</span> | Takes and resolves tickets. Full authority over the tickets assigned to them. |
| Support manager | <span class="ui">Support manager</span> | Supervises the team: can act on every ticket, reassign between agents, receives escalations, views reports. |
| Administrator | <span class="ui">Administrator</span> | Administers the module: activation of support accounts, categories, public holidays of the SLA calendar. Can act on every ticket, but cannot be assigned a ticket. |

Everyone signs in with their Maplenou account (same phone number and password as in the Maplenou app). A Maplenou account has no support role: an administrator gives one by phone number in the <span class="ui">Support roles</span> section of the <span class="ui">Agents</span> screen, and it applies at the person's next sign-in. Administrators of the Maplenou platform are automatically <span class="ui">Administrator</span>. See [Administration](administration.md#support-roles-of-maplenou-accounts).

## Requesters

Requesters are authenticated **customers** (<span class="ui">Customer</span>), **sellers** (<span class="ui">Seller</span>) and **delivery drivers** (<span class="ui">Delivery driver</span>). They are the only ones who create tickets, from their own area (BR-003). A requester:

- sees only their own tickets;
- can write in the conversation and attach files as long as their ticket is not <span class="ui">Closed</span>;
- chooses the priority <span class="ui">Low</span>, <span class="ui">Medium</span> or <span class="ui">High</span> and can report urgency, but can never choose <span class="ui">Critical</span> (BR-036, BR-037);
- can request reopening of their own <span class="ui">Resolved</span> ticket, never of a <span class="ui">Closed</span> one (BR-048, BR-052);
- sees the status, priority, category, their order or delivery reference and the first name of the agent handling their ticket;
- never sees internal notes, SLA data, payment, seller or refund details, or the internal details of the history.

## Permissions table

This table follows the approved authorization matrix, refined by how the application actually works. "Own tickets" means the tickets assigned to the agent; "unassigned" means tickets nobody has taken yet.

| Action | Requester | Support agent | Support manager | Administrator |
| --- | :---: | :---: | :---: | :---: |
| View the queue and unassigned tickets | — | ✓ | ✓ | ✓ |
| View another agent's ticket | — | ✓ (read only) | ✓ | ✓ |
| View internal notes, context and SLA | — | ✓ | ✓ | ✓ |
| Take an unassigned ticket | — | ✓ | ✓ | — (not assignable) |
| Assign or reassign to an agent | — | — | ✓ | ✓ |
| Reply to the requester (visible message) | Own ticket | Own tickets | ✓ | ✓\* |
| Add an internal note | — | Own and unassigned tickets | ✓ | ✓\* |
| Change category or priority | — | Own and unassigned tickets | ✓ | ✓\* |
| Set the <span class="ui">Critical</span> priority | — | Own and unassigned tickets | ✓ | ✓ |
| Change status, mark resolved, close | Limited\*\* | Own tickets | ✓ | ✓\* |
| Add an attachment | Own ticket | Wherever they can write | ✓ | ✓\* |
| Escalate to the manager | — | Own and unassigned tickets | ✓ | ✓ |
| Request reopening | Own resolved ticket | — | — | — |
| Approve or refuse a reopening | — | Own tickets | ✓ | ✓\* |
| View reports | — | — | ✓ | ✓ |
| View the administration pages | — | — | ✓ (read only) | ✓ |
| Activate or deactivate a support account, edit categories and public holidays | — | — | — | ✓ |
| Manage users and roles | — | — | — | ✓ (accounts in Maplenou; support roles in <span class="ui">Support roles</span>) |

\* Administrative capability subject to least privilege: an administrator acts on a ticket only when necessary.
\*\* The requester does not change the status directly: their reply to a <span class="ui">Waiting for customer</span> ticket automatically moves it back to <span class="ui">In progress</span>, and they can request reopening.

On a <span class="ui">Closed</span> ticket no action is possible, whatever the role: the ticket can still be viewed.

## What an agent can do depending on the ticket

=== "Ticket assigned to me"

    Full operational authority (BR-056): reply, add internal notes, change the priority (including <span class="ui">Critical</span>) and category, change the status, mark resolved, close, escalate, approve or refuse a reopening.

=== "Unassigned ticket"

    You can take it with <span class="ui">Take over</span> (BR-057). Before taking it you can already triage it: change the priority (including <span class="ui">Critical</span>) and category, add an internal note, escalate it. To **reply to the requester or change the status, you must take the ticket first**.

=== "A colleague's ticket"

    You can view all of it (conversation, internal notes, history, context, SLA) for support purposes (BR-069), but the screen shows the <span class="ui">Read only</span> box: only the assigned agent or a manager can change it (BR-070). You cannot reassign it (BR-058); if needed, ask a manager.

!!! note "Pending approval"
    An agent's triage right on an **unassigned** ticket (priority, category, internal note, escalation before taking it) is an interpretation of the rule "tickets they are authorized to handle" (BR-056). The application applies it, but it awaits confirmation by the project owner (decision DEC-CS03-08).

## What managers and administrators can also do

- **Support manager**: acts on every ticket as if it were assigned to them. They can assign tickets to themselves (<span class="ui">Assign to me</span>, even when already assigned) or assign them to an active agent with <span class="ui">Assign to an agent</span> (BR-059). They receive new-ticket and escalation notifications. They see the <span class="ui">Reports</span>, <span class="ui">Agents</span>, <span class="ui">Categories</span> and <span class="ui">SLA calendar</span> menus, the last three in read-only mode.
- **Administrator**: same operational rights as a manager, except that they cannot receive a ticket (only active agents and managers are assignable). They are the only ones who can activate or deactivate a support account, edit categories and manage public holidays (BR-065).

## If an action is refused

The application only shows the buttons you are allowed to use on this ticket. If an action is still refused (for example because a manager has just reassigned the ticket), a message is shown, such as "You are not allowed to perform this action." Reload the page to see the actions now available. If you think you need a right, talk to your support manager.
