package ru.jamsys.core.jt;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.flat.template.jdbc.DataMapper;
import ru.jamsys.core.flat.template.jdbc.JdbcRequestRepository;
import ru.jamsys.core.flat.template.jdbc.JdbcTemplate;
import ru.jamsys.core.flat.template.jdbc.StatementType;

import java.math.BigDecimal;
import java.sql.Timestamp;

public enum JTSubscriber implements JdbcRequestRepository {

    SELECT("""
            SELECT
                *
            FROM subscriber
            WHERE
                id_chat = ${IN.id_chat::NUMBER}
                AND bot = ${IN.bot::VARCHAR}
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    SELECT_NOT_REMOVE("""
            SELECT
                *
            FROM subscriber
            WHERE
            remove = 0
            AND bot = ${IN.bot::VARCHAR}
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    UPDATE_REMOVE("""
            UPDATE
                subscriber
            SET
                remove = ${IN.remove::NUMBER},
                ts_update = now()::timestamp
            WHERE
                id_chat = ${IN.id_chat::NUMBER}
                AND bot = ${IN.bot::VARCHAR}
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    INSERT("""
            INSERT INTO subscriber (
                bot,
                id_chat,
                user_info,
                playload,
                remove
            )
            VALUES (
                ${IN.bot::VARCHAR},
                ${IN.id_chat::NUMBER},
                ${IN.user_info::VARCHAR},
                ${IN.playload::VARCHAR},
                0
            )
            ON CONFLICT DO NOTHING
            """, StatementType.SELECT_WITH_AUTO_COMMIT);

    private final JdbcTemplate jdbcTemplate;

    @Getter
    @Setter
    public static class Row extends DataMapper<Row> {
        BigDecimal id;
        String bot;
        BigDecimal idChat;
        Timestamp tsAdd;
        String userInfo;
        String playload;
        BigDecimal remove;
        BigDecimal tsUpdate;
    }

    JTSubscriber(String sql, StatementType statementType) {
        jdbcTemplate = new JdbcTemplate(sql, statementType);
    }

    @Override
    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

}
