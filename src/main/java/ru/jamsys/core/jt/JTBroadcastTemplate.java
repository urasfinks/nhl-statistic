package ru.jamsys.core.jt;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.flat.template.jdbc.DataMapper;
import ru.jamsys.core.flat.template.jdbc.JdbcRequestRepository;
import ru.jamsys.core.flat.template.jdbc.JdbcTemplate;
import ru.jamsys.core.flat.template.jdbc.StatementType;

import java.math.BigDecimal;

public enum JTBroadcastTemplate implements JdbcRequestRepository {

    SELECT("""
            SELECT
                *
            FROM broadcast_template
            WHERE
                key = ${IN.key::VARCHAR}
            """, StatementType.SELECT_WITH_AUTO_COMMIT),
    UPDATE("""
            UPDATE broadcast_template SET
                telegram_notification = ${IN.telegram_notification::VARCHAR}
            WHERE
                key = ${IN.key::VARCHAR}
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    ;

    private final JdbcTemplate jdbcTemplate;

    JTBroadcastTemplate(String sql, StatementType statementType) {
        jdbcTemplate = new JdbcTemplate(sql, statementType);
    }

    @Getter
    @Setter
    public static class Row extends DataMapper<Row> {
        BigDecimal id;
        String key;
        String telegramNotification;
    }

    @Override
    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

}
