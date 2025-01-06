package ru.jamsys.core.jt;

import ru.jamsys.core.flat.template.jdbc.JdbcRequestRepository;
import ru.jamsys.core.flat.template.jdbc.JdbcTemplate;
import ru.jamsys.core.flat.template.jdbc.StatementType;

public enum JTOviSubscriber implements JdbcRequestRepository {

    SELECT("""
            SELECT
                *
            FROM ovi_subscriber
            WHERE
                id_chat = ${IN.id_chat::NUMBER}
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    VOTE("""
            SELECT
                vote,
                count(*)
            FROM ovi_subscriber
            GROUP BY vote
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    SELECT_ALL("""
            SELECT
                *
            FROM ovi_subscriber
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    UPDATE_REMOVE("""
            UPDATE
                ovi_subscriber
            SET
                remove = ${IN.remove::NUMBER}
            WHERE
                id_chat = ${IN.id_chat::NUMBER}
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    UPDATE_VOTE("""
            UPDATE
                ovi_subscriber
            SET
                vote = ${IN.vote::VARCHAR}
            WHERE
                id_chat = ${IN.id_chat::NUMBER}
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    INSERT("""
            INSERT INTO ovi_subscriber (
                id_chat,
                user_info,
                playload,
                remove
            )
            VALUES (
                ${IN.id_chat::NUMBER},
                ${IN.user_info::VARCHAR},
                ${IN.playload::VARCHAR},
                0
            )
            ON CONFLICT DO NOTHING
            """, StatementType.SELECT_WITH_AUTO_COMMIT);

    private final JdbcTemplate jdbcTemplate;

    JTOviSubscriber(String sql, StatementType statementType) {
        jdbcTemplate = new JdbcTemplate(sql, statementType);
    }

    @Override
    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

}
