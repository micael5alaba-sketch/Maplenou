package com.maplenou.backend.returns.dto;

import com.maplenou.backend.returns.ProductCondition;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateReturnRequest(
        @NotNull(message = "L'identifiant de la sous-commande est obligatoire")
        UUID subOrderId,

        @NotBlank(message = "La raison du retour est obligatoire")
        @Size(min = 10, max = 1000, message = "La raison doit contenir entre 10 et 1000 caractères")
        String reason,

        @NotNull(message = "L'état du produit est obligatoire")
        ProductCondition productCondition
) {}
