package com.maplenou.support.ticket.adapter.out.persistence;

import com.maplenou.support.shared.Role;
import com.maplenou.support.ticket.domain.PermissionRule;
import com.maplenou.support.ticket.domain.PermissionScope;
import com.maplenou.support.ticket.domain.TicketAction;
import com.maplenou.support.ticket.domain.vocabulary.PriorityDefinition;
import com.maplenou.support.ticket.domain.vocabulary.StatusDefinition;
import com.maplenou.support.ticket.domain.vocabulary.StatusKind;
import com.maplenou.support.ticket.domain.vocabulary.Transition;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "ticket_statuses")
class StatusEntity {
    @Id
    String code;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    StatusKind kind;
    @Column(name = "label_fr", nullable = false)
    String labelFr;
    @Column(name = "label_en")
    String labelEn;
    @Column(name = "sort_order", nullable = false)
    short sortOrder;
    @Column(name = "sla_paused", nullable = false)
    boolean slaPaused;
    @Column(nullable = false)
    boolean system;
    @Column(nullable = false)
    boolean active;

    protected StatusEntity() {
    }

    StatusEntity(StatusDefinition s) {
        this.code = s.code();
        copyFrom(s);
    }

    void copyFrom(StatusDefinition s) {
        this.kind = s.kind();
        this.labelFr = s.labelFr();
        this.labelEn = s.labelEn();
        this.sortOrder = (short) s.sortOrder();
        this.slaPaused = s.slaPaused();
        this.system = s.system();
        this.active = s.active();
    }

    StatusDefinition toDomain() {
        return new StatusDefinition(code, kind, labelFr, labelEn, sortOrder, slaPaused, system, active);
    }
}

@Embeddable
class TransitionId implements Serializable {
    @Column(name = "from_code")
    String from;
    @Column(name = "to_code")
    String to;

    protected TransitionId() {
    }

    TransitionId(String from, String to) {
        this.from = from;
        this.to = to;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TransitionId t && Objects.equals(from, t.from) && Objects.equals(to, t.to);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to);
    }
}

@Entity
@Table(name = "ticket_transitions")
class TransitionEntity {
    @EmbeddedId
    TransitionId id;

    protected TransitionEntity() {
    }

    TransitionEntity(Transition t) {
        this.id = new TransitionId(t.from(), t.to());
    }

    Transition toDomain() {
        return new Transition(id.from, id.to);
    }
}

@Entity
@Table(name = "ticket_priorities")
class PriorityEntity {
    @Id
    String code;
    @Column(nullable = false)
    short rank;
    @Column(name = "label_fr", nullable = false)
    String labelFr;
    @Column(name = "label_en")
    String labelEn;
    @Column(name = "requester_selectable", nullable = false)
    boolean requesterSelectable;
    @Column(name = "is_default", nullable = false)
    boolean defaultPriority;
    @Column(nullable = false)
    boolean system;
    @Column(nullable = false)
    boolean active;

    protected PriorityEntity() {
    }

    PriorityEntity(PriorityDefinition p) {
        this.code = p.code();
        copyFrom(p);
    }

    void copyFrom(PriorityDefinition p) {
        this.rank = (short) p.rank();
        this.labelFr = p.labelFr();
        this.labelEn = p.labelEn();
        this.requesterSelectable = p.requesterSelectable();
        this.defaultPriority = p.defaultPriority();
        this.system = p.system();
        this.active = p.active();
    }

    PriorityDefinition toDomain() {
        return new PriorityDefinition(code, rank, labelFr, labelEn, requesterSelectable, defaultPriority, system, active);
    }
}

@Embeddable
class PermissionRuleId implements Serializable {
    @Enumerated(EnumType.STRING)
    Role role;
    @Enumerated(EnumType.STRING)
    TicketAction action;
    @Enumerated(EnumType.STRING)
    PermissionScope scope;

    protected PermissionRuleId() {
    }

    PermissionRuleId(Role role, TicketAction action, PermissionScope scope) {
        this.role = role;
        this.action = action;
        this.scope = scope;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PermissionRuleId p && role == p.role && action == p.action && scope == p.scope;
    }

    @Override
    public int hashCode() {
        return Objects.hash(role, action, scope);
    }
}

@Entity
@Table(name = "permission_rules")
class PermissionRuleEntity {
    @EmbeddedId
    PermissionRuleId id;

    protected PermissionRuleEntity() {
    }

    PermissionRuleEntity(PermissionRule r) {
        this.id = new PermissionRuleId(r.role(), r.action(), r.scope());
    }

    PermissionRule toDomain() {
        return new PermissionRule(id.role, id.action, id.scope);
    }
}
