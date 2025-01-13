package ru.jamsys.core.jt;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.flat.template.jdbc.DataMapper;
import ru.jamsys.core.flat.template.jdbc.JdbcRequestRepository;
import ru.jamsys.core.flat.template.jdbc.JdbcTemplate;
import ru.jamsys.core.flat.template.jdbc.StatementType;

import java.math.BigDecimal;
import java.sql.Timestamp;

public enum JTTeamScheduler implements JdbcRequestRepository {

    SELECT("""
            SELECT
                *
            FROM team_scheduler
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    INSERT("""
            INSERT INTO team_scheduler (
                id_team,
                id_game,
                time_game_start,
                game_about,
                json
            )
            VALUES (
                ${IN.id_team::NUMBER},
                ${IN.id_game::VARCHAR},
                ${IN.time_game_start::TIMESTAMP},
                ${IN.game_about::VARCHAR},
                ${IN.json::VARCHAR}
            )
            --ON CONFLICT DO NOTHING
            """, StatementType.SELECT_WITH_AUTO_COMMIT);

    private final JdbcTemplate jdbcTemplate;

    @Getter
    @Setter
    public static class Row extends DataMapper<Row> {
        BigDecimal id;
        String idTeam;
        String idGame;
        Timestamp tsAdd;
        Timestamp timeGameStart;
        String gameAbout;
        String json;
    }

    JTTeamScheduler(String sql, StatementType statementType) {
        jdbcTemplate = new JdbcTemplate(sql, statementType);
    }

    @Override
    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

}
