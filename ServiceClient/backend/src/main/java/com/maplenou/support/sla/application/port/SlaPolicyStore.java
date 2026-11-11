package com.maplenou.support.sla.application.port;

import com.maplenou.support.sla.domain.SlaPolicy;

import java.util.List;

/** SLA targets per priority (table {@code sla_policies}). */
public interface SlaPolicyStore {

    List<SlaPolicy> allPolicies();

    void save(SlaPolicy policy);
}
