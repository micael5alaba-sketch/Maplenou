/**
 * Ticket domain: the aggregate, its lifecycle and the access policy. Pure Java — no Spring, no JPA, no HTTP
 * (enforced by {@code ArchitectureTest}). The vocabulary (statuses, transitions, priorities, permissions) is data,
 * loaded by the application layer and passed in as value objects ({@code vocabulary} package).
 */
package com.maplenou.support.ticket.domain;
