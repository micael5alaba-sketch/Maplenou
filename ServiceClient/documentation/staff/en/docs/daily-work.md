# Daily work

The support area is organised around a side menu. The <span class="ui">Support</span> section contains <span class="ui">Dashboard</span>, <span class="ui">Queue</span>, <span class="ui">My tickets</span> and <span class="ui">Notifications</span>. Managers and administrators also see the <span class="ui">Administration</span> section (<span class="ui">Reports</span>, <span class="ui">Agents</span>, <span class="ui">Categories</span>, <span class="ui">SLA calendar</span>). Badges show the number of open tickets (<span class="ui">Queue</span>), your open tickets (<span class="ui">My tickets</span>) and your unread notifications.

## The dashboard

![An agent's dashboard](images/dashboard.jpg)

*Marks on the screenshot:*

1. The counters: open, unassigned, in progress, waiting for the customer, resolved today, SLA breached, satisfaction. Each one opens the filtered queue.
2. <span class="ui">Open tickets by priority</span> (then by category and the recent tickets).
3. <span class="ui">Queue</span>: every ticket, with the number to handle.
4. <span class="ui">My tickets</span>: the tickets assigned to you.

The <span class="ui">Dashboard</span> shows the workload of the **whole team** (not just yours). Each tile is clickable and opens the queue already filtered.

| Tile | What it counts | Click |
| --- | --- | --- |
| <span class="ui">Open tickets</span> | Tickets that are <span class="ui">New</span>, <span class="ui">Assigned</span>, <span class="ui">In progress</span> or <span class="ui">Waiting for customer</span>. The note "N new" gives the number of <span class="ui">New</span> tickets. | Queue filtered on <span class="ui">Open (all)</span> |
| <span class="ui">Unassigned</span> | Open tickets nobody has taken ("to pick up"). | Queue filtered on <span class="ui">Unassigned</span> |
| <span class="ui">In progress</span> | Tickets with the status <span class="ui">In progress</span>. | Queue filtered on that status |
| <span class="ui">Waiting for customer</span> | Tickets with the status <span class="ui">Waiting for customer</span>. | Queue filtered on that status |
| <span class="ui">Resolved today</span> | Tickets marked resolved since midnight (Lomé time). | Queue filtered on <span class="ui">Resolved</span> (all resolved tickets) |
| <span class="ui">SLA breached</span> | Open tickets whose first-response or resolution deadline has passed. The note "N escalated" gives the number of open escalated tickets. | Queue filtered on <span class="ui">SLA breached</span> |

Below the tiles:

- <span class="ui">Open tickets by priority</span>: breakdown of open tickets between <span class="ui">Critical</span>, <span class="ui">High</span>, <span class="ui">Medium</span> and <span class="ui">Low</span>;
- <span class="ui">Open tickets by category</span>: breakdown of open tickets by category;
- <span class="ui">Recent tickets</span>: the five most recently created open tickets, with their status and an SLA badge when they are no longer <span class="ui">On track</span>. <span class="ui">See all</span> opens the queue.

The <span class="ui">Open the queue</span> button at the top of the screen goes straight to the queue.

## The queue

The <span class="ui">Queue</span> lists every ticket visible to support, 20 per page, newest first by default. The columns are <span class="ui">No.</span>, <span class="ui">Subject</span> (with the <span class="ui">Escalated</span> badge when relevant), <span class="ui">Requester</span> (with their role), <span class="ui">Category</span>, <span class="ui">Status</span>, <span class="ui">Priority</span>, <span class="ui">SLA</span>, <span class="ui">Assigned to</span> and <span class="ui">Created</span>. Click the number or the subject to open the ticket.

The <span class="ui">SLA</span> column shows the worse of the first-response and resolution states (see [SLA and calendar](sla.md)).

## Searching and filtering the queue

![The queue (support manager)](images/queue.jpg)

*Marks on the screenshot:*

1. <span class="ui">Search</span>: a full number, or part of the number or subject.
2. The filters: <span class="ui">Category</span>, <span class="ui">Status</span>, <span class="ui">Priority</span>, <span class="ui">Assigned to</span>, <span class="ui">Requester role</span>.
3. <span class="ui">SLA breached</span> and <span class="ui">Escalated</span>: only late or escalated tickets.
4. <span class="ui">Export (CSV)</span>: the filtered list in a file (managers and administrators).
5. A row: number, subject, requester, category, status, priority, SLA state, agent and date. Click the number or subject to open the ticket.

| Control | Use |
| --- | --- |
| <span class="ui">Search</span> | Type a full number (for example `TKT-2026-00042`) to find exactly that ticket, or part of the number or subject, then confirm. A ticket imported from the former support is also found by its old number (`TKT-2026-000017`: 6 digits, see [Former Maplenou tickets](administration.md#former-maplenou-tickets)). |
| <span class="ui">Category</span> | One ticket category. |
| <span class="ui">Status</span> | One specific status, or <span class="ui">Open (all)</span> for every ticket that is neither resolved nor closed. |
| <span class="ui">Priority</span> | <span class="ui">Critical</span>, <span class="ui">High</span>, <span class="ui">Medium</span> or <span class="ui">Low</span>. |
| <span class="ui">Assigned to</span> | <span class="ui">Me</span>, <span class="ui">Unassigned</span> or a named agent. |
| <span class="ui">Requester role</span> | <span class="ui">Customer</span>, <span class="ui">Seller</span> or <span class="ui">Delivery driver</span>. |
| <span class="ui">SLA breached</span> (checkbox) | Only open tickets with a passed SLA deadline. |
| <span class="ui">Escalated</span> (checkbox) | Only escalated tickets. |
| <span class="ui">Sort</span> | <span class="ui">Newest</span>, <span class="ui">Oldest</span>, <span class="ui">Priority</span> (Critical first) or <span class="ui">Last activity</span>. |
| <span class="ui">Reset</span> | Clears all filters. |

Filters combine (for example <span class="ui">Unassigned</span> + <span class="ui">Priority</span> <span class="ui">High</span>). If nothing matches, the screen shows "No tickets match these filters".

!!! tip "Saving a view"
    Filters are written into the page address: bookmark a filtered view or send its link to a colleague so they see the same list.

## My tickets

<span class="ui">My tickets</span> shows the same list as the queue, limited to the tickets assigned to you. All filters are available except <span class="ui">Assigned to</span>. To see only the remaining work, choose <span class="ui">Status</span> → <span class="ui">Open (all)</span>.

## Notifications

The <span class="ui">Notifications</span> screen gathers in-app notifications. The <span class="ui">All</span> and <span class="ui">Unread</span> tabs filter the list; <span class="ui">Mark all as read</span> clears the counter. Clicking a notification opens the ticket and marks it as read.

| Notification | Who receives it |
| --- | --- |
| New ticket | Active support managers, whenever a ticket is created. |
| Ticket assigned | The agent the ticket has just been assigned to (unless they assigned it to themselves). |
| Requester replied | The assigned agent; if the ticket is unassigned, the active support managers. |
| Reopening request | The assigned agent; if the ticket is unassigned, the active support managers. |
| Ticket escalated | Active support managers. |

You are never notified of your own actions. Notifications are sent after the action has been saved: a notification problem never cancels a successful action on a ticket (BR-020). So keep checking the queue, even without notifications.

## Push notifications in the browser

To be alerted on your computer even when the support workspace is not on screen, turn on push notifications in your browser:

1. Open <span class="ui">Notifications</span> in the side menu.
2. In the <span class="ui">Push notifications</span> card, click <span class="ui">Turn on push notifications</span>, then allow notifications when the browser asks.
3. Click <span class="ui">Send a test notification</span>: a "Notification de test" notification should appear after a few seconds.

Clicking a push notification opens the ticket concerned. Push notification texts are in French. <span class="ui">Turn off</span> stops push notifications on this device; signing out also stops them for this browser, which matters on a shared computer.

If the card says notifications are blocked, click the icon to the left of the site address, allow notifications, then reload the page. If it says the browser does not support them or that they are not configured, the <span class="ui">Notifications</span> screen remains your reference. The mobile apps ask for permission to show notifications themselves.

## Suggested daily routine

This routine is an organisational suggestion, not a rule.

1. **Open the dashboard** and look at the <span class="ui">SLA breached</span> and <span class="ui">Unassigned</span> tiles.
2. **Handle the urgent cases**: queue filtered on <span class="ui">SLA breached</span>, then sorted by <span class="ui">Priority</span>.
3. **Read the unread notifications**, especially requester replies and reopening requests.
4. **Work through <span class="ui">My tickets</span>** (status <span class="ui">Open (all)</span>, sort <span class="ui">Priority</span>): reply, request missing information, mark resolved what is fixed.
5. **Take unassigned tickets** according to your workload, starting with the highest-priority and oldest ones.
6. **Before you leave**, add an internal note on in-progress tickets that need follow-up.

For managers: also watch the <span class="ui">Escalated</span> checkbox and each agent's workload (<span class="ui">Open tickets</span> column of the <span class="ui">Agents</span> screen), and reassign when needed.
