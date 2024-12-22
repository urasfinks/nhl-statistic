package ru.jamsys.core.jt;

import ru.jamsys.core.flat.template.jdbc.JdbcRequestRepository;
import ru.jamsys.core.flat.template.jdbc.JdbcTemplate;
import ru.jamsys.core.flat.template.jdbc.StatementType;

public enum JTPrevGoal implements JdbcRequestRepository {

    SELECT("""
            SELECT
                *
            FROM prev_goal
            WHERE
                id_game = ${IN.id_game::VARCHAR}
                AND id_player = ${IN.id_player::NUMBER}
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    INSERT("""
            INSERT INTO prev_goal (
                id_game,
                id_player,
                prev_goal
            )
            VALUES (
                ${IN.id_game::VARCHAR},
                ${IN.id_player::NUMBER},
                ${IN.prev_goal::NUMBER}
            )
            """, StatementType.SELECT_WITH_AUTO_COMMIT)
    ;

    private final JdbcTemplate jdbcTemplate;

    JTPrevGoal(String sql, StatementType statementType) {
        jdbcTemplate = new JdbcTemplate(sql, statementType);
    }

    @Override
    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

}
