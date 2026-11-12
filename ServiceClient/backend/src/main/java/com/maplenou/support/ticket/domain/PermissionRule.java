package com.maplenou.support.ticket.domain;

import com.maplenou.support.shared.Role;

/** "Role R may perform action A on tickets in scope S" — one row of the permission matrix. */
public record PermissionRule(Role role, TicketAction action, PermissionScope scope) {
}
