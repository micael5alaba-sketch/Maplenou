package com.maplenou.support.demo;

import com.maplenou.support.shared.Actor;
import com.maplenou.support.shared.Role;

import java.util.List;
import java.util.UUID;

/** Demo personas matching the CS-02 mockups. Used by the demo profile and by tests. */
public final class DemoUsers {

    public static final UUID CUSTOMER_JEAN = UUID.fromString("11111111-1111-4111-8111-111111111111");
    public static final UUID SELLER_MARIE = UUID.fromString("22222222-2222-4222-8222-222222222222");
    public static final UUID DRIVER_KOSSI = UUID.fromString("33333333-3333-4333-8333-333333333333");
    public static final UUID AGENT_SOPHIE = UUID.fromString("44444444-4444-4444-8444-444444444444");
    public static final UUID AGENT_PAUL = UUID.fromString("55555555-5555-4555-8555-555555555555");
    public static final UUID MANAGER_AMINA = UUID.fromString("66666666-6666-4666-8666-666666666666");
    public static final UUID ADMIN = UUID.fromString("77777777-7777-4777-8777-777777777777");

    public static final List<Actor> ALL = List.of(
            new Actor(CUSTOMER_JEAN, Role.CUSTOMER, "Jean K.", "jean.k@example.com", "+228 90 00 00 01"),
            new Actor(SELLER_MARIE, Role.SELLER, "Marie K.", "marie.k@example.com", "+228 90 00 00 02"),
            new Actor(DRIVER_KOSSI, Role.DELIVERY_DRIVER, "Kossi D.", "kossi.d@example.com", "+228 90 00 00 03"),
            new Actor(AGENT_SOPHIE, Role.SUPPORT_AGENT, "Sophie N.", "sophie@maplenou.example", null),
            new Actor(AGENT_PAUL, Role.SUPPORT_AGENT, "Paul D.", "paul@maplenou.example", null),
            new Actor(MANAGER_AMINA, Role.SUPPORT_MANAGER, "Amina K.", "amina@maplenou.example", null),
            new Actor(ADMIN, Role.ADMINISTRATOR, "Admin Maplenou", "admin@maplenou.example", null));

    private DemoUsers() {
    }

    public static Actor byId(UUID id) {
        return ALL.stream().filter(u -> u.id().equals(id)).findFirst().orElse(null);
    }
}
