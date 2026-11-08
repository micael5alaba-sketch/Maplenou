package com.maplenou.support.sla.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

interface SlaPeriodJpaRepository extends JpaRepository<SlaPeriodEntity, UUID> {
    List<SlaPeriodEntity> findByTicketIdOrderByPeriodNumberAsc(UUID ticketId);
}

interface HolidayJpaRepository extends JpaRepository<HolidayEntity, LocalDate> {
}

interface SlaPolicyJpaRepository extends JpaRepository<SlaPolicyEntity, String> {
}
