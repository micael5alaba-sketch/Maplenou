package com.maplenou.backend.notification.dto;

import com.maplenou.backend.notification.DevicePlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterTokenRequest(

        @NotBlank
        String token,

        @NotNull
        DevicePlatform platform
) {}
