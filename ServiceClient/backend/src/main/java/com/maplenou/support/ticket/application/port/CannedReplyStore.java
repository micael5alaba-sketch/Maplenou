package com.maplenou.support.ticket.application.port;

import com.maplenou.support.ticket.domain.CannedReply;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CannedReplyStore {

    List<CannedReply> all();

    Optional<CannedReply> findReply(UUID id);

    void save(CannedReply reply);

    void delete(UUID id);
}
