# CS-01 Authorization Matrix

**Status:** Approved | **Version:** 1.0.0

Authorization combines role permissions with ticket context and ownership/assignment. Role membership alone does not imply unrestricted access.

| Action | Customer | Seller | Driver | Support Agent | Support Manager | Administrator |
| --- | :---: | :---: | :---: | :---: | :---: | :---: |
| Create ticket | ✓ | ✓ | ✓ | — | — | ✓* |
| View own tickets | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| View assigned tickets | — | — | — | ✓ | ✓ | ✓ |
| View support queue | — | — | — | ✓ | ✓ | ✓ |
| View another agent's ticket | — | — | — | ✓ | ✓ | ✓ |
| Send customer-visible message | ✓ | ✓ | ✓ | ✓ | ✓ | ✓* |
| Add internal note | — | — | — | ✓ | ✓ | ✓* |
| Change category | — | — | — | ✓ | ✓ | ✓* |
| Change priority | — | — | — | ✓ | ✓ | ✓* |
| Set `CRITICAL` | — | — | — | ✓ | ✓ | ✓ |
| Assign unassigned ticket to self | — | — | — | ✓ | ✓ | ✓ |
| Reassign another agent's ticket | — | — | — | — | ✓ | ✓ |
| Change valid status | Limited | Limited | Limited | ✓ | ✓ | ✓* |
| Add attachment | ✓ | ✓ | ✓ | ✓ | ✓ | ✓* |
| Resolve ticket | — | — | — | ✓ | ✓ | ✓* |
| Close ticket | — | — | — | ✓ | ✓ | ✓* |
| Request reopen of own resolved ticket | ✓ | ✓ | ✓ | — | — | — |
| Approve reopening | — | — | — | ✓ | ✓ | ✓* |
| Escalate to manager | — | — | — | ✓ | ✓ | ✓ |
| Rate own resolved/closed ticket (once) | ✓ | ✓ | ✓ | — | — | — |
| Apply/dismiss AI suggestion, ask for a reply draft | — | — | — | ✓ (assigned or unassigned) | ✓ | ✓ |
| Ask Maplenou for a return, release a delivery, message the courier | — | — | — | ✓ (assigned) | ✓ | ✓ |
| Use canned replies | — | — | — | ✓ | ✓ | ✓ |
| Manage canned replies | — | — | — | — | ✓ | ✓ |
| Export tickets as CSV | — | — | — | — | ✓ | ✓ |
| Edit statuses, priorities, SLA targets, permissions, notification texts, add categories | — | — | — | — | — | ✓ |
| Manage users/roles | — | — | — | — | — | ✓ |
| Modify system configuration | — | — | — | — | — | ✓ |

`✓*` indicates an administrative capability that remains subject to explicit authorization policy and least privilege.

## Context constraints

- External requesters can access only tickets for which they are authorized participants/requesters.
- Support Agents can modify tickets assigned to them or otherwise authorized for them.
- Viewing another agent's ticket does not grant modification or reassignment rights.
- Internal notes are support-only.
- Contextual order/payment/delivery/seller/refund data requires separate authorization.
- Unauthorized actions are denied by default.

## Configurable matrix (DEC-CS11-02)

Since CS-11 the ticket-level part of this matrix is data (`permission_rules`: role × action × scope OWN / ASSIGNED /
UNASSIGNED / ANY), shown and edited by administrators on « Droits » (`/support/admin/permissions`). The table above is
the default (V4 seed and `PermissionMatrix.defaults()`); the backend refuses any change that would let a requester see
another person's ticket or an internal note, or remove the administrators' configuration rights
(`INVALID_PERMISSIONS`). State guards (no message on a closed ticket, reopening only from resolved…) are not
configurable.
