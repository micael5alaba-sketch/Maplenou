# Maplenou Customer Service — Functional Requirements

**ID:** CS-01-FR | **Status:** Approved | **Version:** 1.0.0

## 1. Purpose

Define the functional behavior of the Customer Service and Ticket Management module before implementation.

## 2. Product objective

Provide a controlled support-ticket lifecycle for customers, sellers, and delivery drivers, managed by support agents, support managers, and administrators.

## 3. Actors

| Actor | Responsibility |
| --- | --- |
| Customer | Requests assistance |
| Seller | Requests seller support |
| Delivery Driver | Reports delivery incidents |
| Support Agent | Handles and resolves tickets |
| Support Manager | Supervises and escalates |
| Administrator | Performs administration |

## 4. Functional requirements

- **FR-001 Ticket creation:** authenticated customers, sellers, and delivery drivers can create tickets.
- **FR-002 Automatic metadata:** the system records technical ID, unique ticket number, requester, actor, timestamps, initial status, and applicable SLA targets.
- **FR-003 Categories:** Order, Payment, Delivery, Seller, Account, Return/Refund, Other.
- **FR-004 Priorities:** Low, Medium, High, Critical.
- **FR-005 Statuses:** New, Assigned, In Progress, Waiting for Customer, Resolved, Closed.
- **FR-006 Assignment:** authorized support users can assign a ticket to an active support agent.
- **FR-007 Messages:** authorized participants can exchange customer-visible messages.
- **FR-008 Internal notes:** authorized support personnel can create notes invisible to external requesters.
- **FR-009 History:** significant mutations are auditable and normal support operations cannot edit history.
- **FR-010 Attachments:** authorized participants can add validated attachments within approved limits.
- **FR-011 Context:** tickets may reference orders, payments, sellers, deliveries, and refunds subject to authorization.
- **FR-012 Access:** users only access tickets and operations for which they are authorized; support access follows role and ticket-context permissions.
- **FR-013 Notifications:** support events can trigger notifications; notification failure must not corrupt core ticket state.
- **FR-014 Search/filter:** support users can filter by ticket number, status, priority, category, agent, requester, and date.
- **FR-015 Resolution/closure:** authorized users can resolve and close tickets according to the lifecycle policy.
- **FR-016 SLA tracking:** the system tracks first-response and resolution targets from ticket creation according to priority and business hours. *Amended 2026-10-29 by DEC-CS10-16: counted 24/7 by default (`SLA_MODE`).*
- **FR-017 Reopening:** requesters can request reopening of their own resolved tickets; authorized support personnel decide whether to reopen.

## 5. Explicit MVP exclusions

AI chatbot, AI-generated replies, predictive classification, sentiment analysis, ML escalation, predictive SLA analytics, autonomous support decisions, and advanced support analytics are outside the MVP unless separately approved.
