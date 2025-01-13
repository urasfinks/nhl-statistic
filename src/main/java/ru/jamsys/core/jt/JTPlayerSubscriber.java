package ru.jamsys.core.jt;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.flat.template.jdbc.DataMapper;
import ru.jamsys.core.flat.template.jdbc.JdbcRequestRepository;
import ru.jamsys.core.flat.template.jdbc.JdbcTemplate;
import ru.jamsys.core.flat.template.jdbc.StatementType;

import java.math.BigDecimal;
import java.sql.Timestamp;

public enum JTPlayerSubscriber implements JdbcRequestRepository {

    SELECT("""
            SELECT
                *
            FROM player_subscriber
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    SELECT_MY("""
            SELECT
                *
            FROM player_subscriber
            WHERE
                id_chat = ${IN.id_chat::NUMBER}
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    INSERT("""
            INSERT INTO player_subscriber (
                id_chat,
                id_player
            )
            VALUES (
                ${IN.id_chat::NUMBER},
                ${IN.id_chat::NUMBER}
            )
            ON CONFLICT DO NOTHING
            """, StatementType.SELECT_WITH_AUTO_COMMIT);

    @Getter
    @Setter
    public static class Row extends DataMapper<Row> {
        BigDecimal id;
        Timestamp tsAdd;
        BigDecimal idChat;
        BigDecimal idPlayer;
    }

    private final JdbcTemplate jdbcTemplate;

    JTPlayerSubscriber(String sql, StatementType statementType) {
        jdbcTemplate = new JdbcTemplate(sql, statementType);
    }

    @Override
    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

}
