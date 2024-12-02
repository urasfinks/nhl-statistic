package ru.jamsys.core.jt;

import ru.jamsys.core.flat.template.jdbc.JdbcRequestRepository;
import ru.jamsys.core.flat.template.jdbc.JdbcTemplate;
import ru.jamsys.core.flat.template.jdbc.StatementType;

public enum JTHttpCache implements JdbcRequestRepository {

    SELECT("""
            SELECT * FROM http_cache
            WHERE
                url = ${IN.url::VARCHAR};
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    INSERT("""
            INSERT INTO http_cache (url, data)
            VALUES (${IN.url::VARCHAR}, ${IN.data::VARCHAR})
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    UPDATE("""
            UPDATE http_cache
            SET
                data = ${IN.data::VARCHAR},
                time_add = now()::timestamp
            WHERE
                url = ${IN.url::VARCHAR};
            """, StatementType.SELECT_WITH_AUTO_COMMIT);

    private final JdbcTemplate jdbcTemplate;

    JTHttpCache(String sql, StatementType statementType) {
        jdbcTemplate = new JdbcTemplate(sql, statementType);
    }

    @Override
    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

}
