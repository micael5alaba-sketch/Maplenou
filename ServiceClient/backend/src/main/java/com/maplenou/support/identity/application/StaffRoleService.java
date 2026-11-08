package com.maplenou.support.identity.application;

import com.maplenou.support.identity.application.port.StaffAssignmentStore;
import com.maplenou.support.identity.domain.MaplenouRoles;
import com.maplenou.support.identity.domain.StaffAssignment;
import com.maplenou.support.shared.Actor;
import com.maplenou.support.shared.Role;
import com.maplenou.support.shared.SupportException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Support roles of Maplenou accounts (DEC-CS10-17). Maplenou accounts have no support role: an administrator gives
 * one by phone number; it applies at the person's next sign-in.
 */
@Service
public class StaffRoleService {

    private static final Set<Role> STAFF = Set.of(Role.SUPPORT_AGENT, Role.SUPPORT_MANAGER, Role.ADMINISTRATOR);

    private final StaffAssignmentStore staff;
    private final Clock clock;

    public StaffRoleService(StaffAssignmentStore staff, Clock clock) {
        this.staff = staff;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<StaffAssignment> list() {
        return staff.all().stream().sorted(Comparator.comparing(StaffAssignment::phone)).toList();
    }

    @Transactional(readOnly = true)
    public Role roleOf(String phone) {
        String normalized = MaplenouRoles.normalizePhone(phone);
        return normalized == null ? null : staff.find(normalized).map(StaffAssignment::role).orElse(null);
    }

    @Transactional
    public StaffAssignment assign(Actor admin, String phone, Role role) {
        String normalized = MaplenouRoles.normalizePhone(phone);
        if (normalized == null) {
            throw SupportException.badRequest("INVALID_PHONE", "Numéro de téléphone invalide.", null);
        }
        if (!STAFF.contains(role)) {
            throw SupportException.badRequest("INVALID_STAFF_ROLE",
                    "Rôle support attendu : agent, responsable ou administrateur.", null);
        }
        StaffAssignment a = new StaffAssignment(normalized, role, admin.id(), clock.instant());
        staff.save(a);
        return a;
    }

    @Transactional
    public void remove(String phone) {
        String normalized = MaplenouRoles.normalizePhone(phone);
        if (normalized != null) {
            staff.delete(normalized);
        }
    }
}
