package com.maplenou.backend.withdrawal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WithdrawalStatusHistoryRepository extends JpaRepository<WithdrawalStatusHistory, UUID> {

    List<WithdrawalStatusHistory> findBySubjectTypeAndSubjectIdOrderByCreatedAtAsc(
            WithdrawalSubjectType subjectType, UUID subjectId);
}
