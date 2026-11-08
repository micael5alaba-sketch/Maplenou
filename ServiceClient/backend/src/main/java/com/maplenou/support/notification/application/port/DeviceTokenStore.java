package com.maplenou.support.notification.application.port;

import com.maplenou.support.notification.domain.DeviceToken;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceTokenStore {

    List<DeviceToken> forUser(UUID userId);

    Optional<DeviceToken> findByToken(String token);

    void save(DeviceToken token);

    void deleteByToken(String token);
}
