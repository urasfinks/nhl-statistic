package ru.jamsys.core.jt;

import ru.jamsys.core.flat.template.jdbc.JdbcRequestRepository;
import ru.jamsys.core.flat.template.jdbc.JdbcTemplate;
import ru.jamsys.core.flat.template.jdbc.StatementType;

public enum JTGameDiff implements JdbcRequestRepository {

    SELECT("""
            SELECT
                *
            FROM game_diff
            WHERE
                id_game = ${IN.id_game::VARCHAR}
            """, StatementType.SELECT_WITH_AUTO_COMMIT),
    INSERT("""
            INSERT INTO game_diff (
                id_game,
                scoring_plays
            )
            VALUES (
                ${IN.id_game::VARCHAR},
                ${IN.scoring_plays::VARCHAR}
            )
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    UPDATE("""
            UPDATE game_diff SET
                scoring_plays = ${IN.scoring_plays::VARCHAR}
            WHERE
                id_game = ${IN.id_game::VARCHAR}
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    ;

    private final JdbcTemplate jdbcTemplate;

    JTGameDiff(String sql, StatementType statementType) {
        jdbcTemplate = new JdbcTemplate(sql, statementType);
    }

    @Override
    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

}
