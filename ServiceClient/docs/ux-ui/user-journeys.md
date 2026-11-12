# CS-02.1 — User Journeys

**Status:** APPROVED (CS-02 final validation)  
**Source of truth:** CS-01 approved requirements, business rules, acceptance criteria, and authorization matrix.

## 1. Customer journey

1. Authenticate.
2. Open Help & Support.
3. Select Create ticket.
4. Enter ticket information.
5. Optionally attach permitted files.
6. Submit.
7. Ticket is created as **NEW** and a ticket number is provided.
8. Customer views authorized ticket information.
9. Support personnel handle the ticket.
10. Customer receives relevant notifications.
11. If more information is required, ticket may enter **WAITING_FOR_CUSTOMER**.
12. Customer replies/provides information.
13. Support resolves the ticket.
14. Customer may request reopening while **RESOLVED**.
15. Support approves or refuses the request.
16. Approved reopening returns the ticket to **IN_PROGRESS**.
17. **CLOSED** cannot be reopened by the requester; a new ticket is required.

## 2. Seller journey

Same ticket interaction model as the customer, with seller-specific authorization for contextual information:

- create a ticket;
- view own tickets;
- send customer-visible messages;
- add permitted attachments;
- provide additional information;
- request reopening of own RESOLVED ticket.

## 3. Delivery-driver journey

Same interaction model, with driver-specific authorization for contextual delivery information:

- create a ticket;
- view own tickets;
- send customer-visible messages;
- add permitted attachments;
- provide additional information;
- request reopening of own RESOLVED ticket.

## 4. Support Agent journey

1. Authenticate.
2. Open support dashboard.
3. View authorized support queue.
4. Search/filter tickets as available.
5. Open a ticket.
6. Review requester and authorized context.
7. Review category, priority, status, assignment, and SLA.
8. Assign an unassigned ticket to self when permitted.
9. Modify category/priority/status within authorization.
10. Send customer-visible message.
11. Add internal note when needed.
12. Request or wait for additional customer information.
13. Resolve the ticket.
14. Close according to the approved workflow.
15. Handle reopening requests within authorization.
16. Significant actions are recorded in ticket history/audit.

A Support Agent may see other agents' tickets in the support queue, but viewing does not grant reassignment or unrestricted modification rights.

## 5. Support Manager journey

Performs the Support Agent workflow plus approved supervisory actions, including reassignment, supervision, reopening decisions, and escalation within authorization.

## 6. Administrator journey

Performs approved administrative capabilities. Administrative role does not imply bypassing explicit authorization.

## 7. Cross-role UX

- Customer-visible messages are visible to authorized participants and support personnel.
- Internal notes are visible only to authorized support personnel.
- Attachments are accessible only to authorized participants/support personnel and are subject to validation.
- Notification failure must not corrupt core ticket state.

## 8. Required feedback states

The UI must clearly communicate success/failure for ticket creation, attachment validation, message sending, status changes, assignment changes, reopening request/decision, resolution, closure, unauthorized actions, and notification failure/retry state where exposed.

## 9. Out of scope

AI chatbot, AI-generated replies, predictive classification, sentiment analysis, ML-based escalation, predictive SLA analytics, and autonomous support decisions remain outside the MVP.
