package ru.jamsys.core.jt;

import ru.jamsys.core.flat.template.jdbc.JdbcRequestRepository;
import ru.jamsys.core.flat.template.jdbc.JdbcTemplate;
import ru.jamsys.core.flat.template.jdbc.StatementType;

public enum JTLogRequest implements JdbcRequestRepository {

    INSERT("""
            INSERT INTO log_request (
                url,
                data
            )
            VALUES (
                ${IN.url::VARCHAR},
                ${IN.data::VARCHAR}
            )
            """, StatementType.SELECT_WITH_AUTO_COMMIT);

    private final JdbcTemplate jdbcTemplate;

    JTLogRequest(String sql, StatementType statementType) {
        jdbcTemplate = new JdbcTemplate(sql, statementType);
    }

    @Override
    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

}
