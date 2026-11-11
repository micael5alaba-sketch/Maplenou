package com.maplenou.support.ticket.domain;

/** Which tickets a permission rule covers, seen from the caller. */
public enum PermissionScope {
    /** Tickets the caller opened (requester). */
    OWN,
    /** Tickets assigned to the caller. */
    ASSIGNED,
    /** Tickets nobody is assigned to. */
    UNASSIGNED,
    /** Every ticket. */
    ANY
}
