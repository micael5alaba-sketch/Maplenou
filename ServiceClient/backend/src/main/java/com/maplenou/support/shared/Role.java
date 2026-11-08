package com.maplenou.support.shared;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Actors from CS-01 (functional-requirements §3, authorization-matrix). */
public enum Role {
    CUSTOMER,
    SELLER,
    DELIVERY_DRIVER,
    SUPPORT_AGENT,
    SUPPORT_MANAGER,
    ADMINISTRATOR;

    /** Precedence when a token carries several roles: the most privileged one wins. */
    private static final List<Role> PRECEDENCE = List.of(
            ADMINISTRATOR, SUPPORT_MANAGER, SUPPORT_AGENT, SELLER, DELIVERY_DRIVER, CUSTOMER);

    /** Customer, seller or delivery driver (BR-003). */
    public boolean isExternalRequester() {
        return this == CUSTOMER || this == SELLER || this == DELIVERY_DRIVER;
    }

    /** Support Agent, Support Manager or Administrator. */
    public boolean isSupportStaff() {
        return this == SUPPORT_AGENT || this == SUPPORT_MANAGER || this == ADMINISTRATOR;
    }

    /** Manager or Administrator: supervisory support authority (BR-059). */
    public boolean isSupervisor() {
        return this == SUPPORT_MANAGER || this == ADMINISTRATOR;
    }

    /** May create tickets (authorization matrix: customer, seller, driver, administrator*). */
    public boolean canCreateTickets() {
        return isExternalRequester() || this == ADMINISTRATOR;
    }

    /** May be the assignee of a ticket (BR-013: active support agent). */
    public boolean isAssignable() {
        return this == SUPPORT_AGENT || this == SUPPORT_MANAGER;
    }

    public static Optional<Role> highest(Collection<String> names) {
        for (Role r : PRECEDENCE) {
            if (names.contains(r.name())) {
                return Optional.of(r);
            }
        }
        return Optional.empty();
    }
}
