package com.maplenou.support.ticket.application;

import com.maplenou.support.identity.application.UserDirectory;
import com.maplenou.support.identity.application.UserDirectory.UserSummary;
import com.maplenou.support.shared.Actor;
import com.maplenou.support.shared.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Support agents with their open-ticket load (assignment dropdown, « Agents » page). */
@Service
public class AgentService {

    public record AgentView(UUID id, String name, String email, Role role, boolean active, long openTickets) {
    }

    private final UserDirectory directory;
    private final TicketStatistics statistics;

    public AgentService(UserDirectory directory, TicketStatistics statistics) {
        this.directory = directory;
        this.statistics = statistics;
    }

    @Transactional(readOnly = true)
    public List<AgentView> assignable() {
        Map<UUID, Long> load = statistics.openLoadByAgent();
        return directory.activeAssignableAgents().stream().map(u -> view(u, load)).toList();
    }

    @Transactional(readOnly = true)
    public List<AgentView> allStaff() {
        Map<UUID, Long> load = statistics.openLoadByAgent();
        return directory.supportStaff().stream().map(u -> view(u, load)).toList();
    }

    @Transactional
    public AgentView setActive(Actor admin, UUID id, boolean active) {
        return view(directory.setActive(admin, id, active), statistics.openLoadByAgent());
    }

    private static AgentView view(UserSummary u, Map<UUID, Long> load) {
        return new AgentView(u.id(), u.name(), u.email(), u.role(), u.active(), load.getOrDefault(u.id(), 0L));
    }
}
