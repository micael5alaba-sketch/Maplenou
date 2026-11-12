package com.maplenou.support.ticket.adapter.in.web;

import com.maplenou.support.platform.security.CurrentActorProvider;
import com.maplenou.support.ticket.application.AgentService;
import com.maplenou.support.ticket.application.AgentService.AgentView;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Support agents with their open-ticket load. */
@RestController
@Tag(name = "Administration")
public class AgentController {

    private final AgentService agents;
    private final CurrentActorProvider currentUser;

    public AgentController(AgentService agents, CurrentActorProvider currentUser) {
        this.agents = agents;
        this.currentUser = currentUser;
    }

    /** Active agents that can receive tickets (reassignment dropdown). */
    @GetMapping("/api/v1/support/agents")
    @PreAuthorize("hasAnyRole('SUPPORT_AGENT','SUPPORT_MANAGER','ADMINISTRATOR')")
    public List<AgentView> assignable() {
        return agents.assignable();
    }

    @GetMapping("/api/v1/support/admin/agents")
    @PreAuthorize("hasAnyRole('SUPPORT_MANAGER','ADMINISTRATOR')")
    public List<AgentView> allStaff() {
        return agents.allStaff();
    }

    public record ActiveFlag(boolean active) {
    }

    @PutMapping("/api/v1/support/admin/agents/{id}/active")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public AgentView setActive(@PathVariable UUID id, @RequestBody ActiveFlag body) {
        return agents.setActive(currentUser.get(), id, body.active());
    }
}
