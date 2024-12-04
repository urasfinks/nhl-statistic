package ru.jamsys.core.jt;

import ru.jamsys.core.flat.template.jdbc.JdbcRequestRepository;
import ru.jamsys.core.flat.template.jdbc.JdbcTemplate;
import ru.jamsys.core.flat.template.jdbc.StatementType;

public enum JTScheduler implements JdbcRequestRepository {

    SELECT("""
            SELECT * FROM scheduler
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    INSERT("""
            INSERT INTO scheduler (
                id_chat,
                id_player,
                id_team,
                id_game,
                time_game_start,
                about
            )
            VALUES (
                ${IN.id_chat::NUMBER},
                ${IN.id_player::NUMBER},
                ${IN.id_team::NUMBER},
                ${IN.id_game::VARCHAR},
                ${IN.time_game_start::TIMESTAMP},
                ${IN.about::VARCHAR}
            )
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    UPDATE("""
            
            """, StatementType.SELECT_WITH_AUTO_COMMIT);

    private final JdbcTemplate jdbcTemplate;

    JTScheduler(String sql, StatementType statementType) {
        jdbcTemplate = new JdbcTemplate(sql, statementType);
    }

    @Override
    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

}
