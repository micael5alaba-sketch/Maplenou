package com.maplenou.support.it;

import com.maplenou.support.shared.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** BR-018: normal operations cannot edit history — enforced by the database itself. */
class HistoryImmutabilityIT extends IntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void historyRowsCannotBeUpdatedOrDeleted() throws Exception {
        String id = createTicket(newUser(Role.CUSTOMER));
        UUID ticketId = UUID.fromString(id);
        assertThatThrownBy(() -> jdbc.update("update ticket_history set action = 'X' where ticket_id = ?", ticketId))
                .isInstanceOf(DataAccessException.class).hasMessageContaining("append-only");
        assertThatThrownBy(() -> jdbc.update("delete from ticket_history where ticket_id = ?", ticketId))
                .isInstanceOf(DataAccessException.class).hasMessageContaining("append-only");
    }
}
