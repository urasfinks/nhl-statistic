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

    SELECT_TEAM_SCHEDULER("""
            SELECT
                *
            FROM team_scheduler
            WHERE
                id_team = ${IN.id_team::NUMBER}
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    DELETE_IS_POSTPONED("""
            DELETE FROM team_scheduler
            WHERE id_game = ${IN.id_game::VARCHAR}
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    DELETE_FINISH_GAME("""
            DELETE
            FROM team_scheduler
            WHERE
                id_game = ${IN.id_game::VARCHAR}
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    SELECT_ACTIVE_GAME("""
            SELECT
                ps1.id_chat,
                ps1.id_player,
                ts1.id_game
            FROM team_scheduler ts1
            INNER JOIN player_subscriber ps1 ON ts1.id_team = ps1.id_team
            WHERE
                time_game_start < now()::timestamp
            ORDER BY 1,2,3 DESC
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    SELECT_INVITE_GAME("""
            SELECT
            	ps1.id_chat,
                ps1.id_player,
                ts1.id_game,
            	ts1.json
            FROM
            	public.team_scheduler ts1
            INNER JOIN player_subscriber ps1 ON ts1.id_team = ps1.id_team
            WHERE
                ts1.send_invite = 0
                AND ts1.time_game_start < (now()::timestamp + interval '12 hour')
            ORDER BY 1,2,3 DESC
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    UPDATE_INVITED_GAME("""
            UPDATE
            	public.team_scheduler
            SET
                send_invite = 1
            WHERE
                id_game = ${IN.id_game::VARCHAR}
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
        BigDecimal sendInvite;
    }

    JTTeamScheduler(String sql, StatementType statementType) {
        jdbcTemplate = new JdbcTemplate(sql, statementType);
    }

    @Override
    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

}
