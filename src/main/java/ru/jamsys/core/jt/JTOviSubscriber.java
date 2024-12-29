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

    DELETE("""
            DELETE
            FROM ovi_subscriber
            WHERE
                id_chat = ${IN.id_chat::NUMBER}
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    UPDATE("""
            UPDATE
                ovi_subscriber
            SET
                vote = ${IN.vote::VARCHAR}
            WHERE
                id_chat = ${IN.id_chat::NUMBER}
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    INSERT("""
            INSERT INTO ovi_subscriber (
                id_chat
            )
            VALUES (
                ${IN.id_chat::NUMBER}
            )
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
