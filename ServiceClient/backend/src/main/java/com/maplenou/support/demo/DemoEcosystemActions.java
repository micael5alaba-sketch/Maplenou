package com.maplenou.support.demo;

import com.maplenou.support.context.application.port.EcosystemActions;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/** Actions accepted without calling anything (demo mode): the ticket records them like real ones. */
public class DemoEcosystemActions implements EcosystemActions {

    private final AtomicInteger counter = new AtomicInteger(100);

    @Override
    public ActionResult requestReturn(String orderRef, String subOrderId, String reason, String productCondition,
                                      ActionContext context) {
        return new ActionResult("RMB-DEMO-" + counter.incrementAndGet(), "PENDING");
    }

    @Override
    public ActionResult rescheduleDelivery(String deliveryRef, String reason, ActionContext context) {
        return new ActionResult(deliveryRef, "PENDING");
    }

    @Override
    public ActionResult messageDriver(UUID driverId, String message, ActionContext context) {
        return new ActionResult(null, "SENT");
    }
}
