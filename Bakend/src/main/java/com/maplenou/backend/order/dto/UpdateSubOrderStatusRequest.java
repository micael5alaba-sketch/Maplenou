package com.maplenou.backend.order.dto;

import com.maplenou.backend.order.SubOrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateSubOrderStatusRequest(
        @NotNull(message = "Le statut est obligatoire")
        SubOrderStatus status
) {}
