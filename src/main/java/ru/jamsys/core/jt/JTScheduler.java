package ru.jamsys.core.jt;

import ru.jamsys.core.flat.template.jdbc.JdbcRequestRepository;
import ru.jamsys.core.flat.template.jdbc.JdbcTemplate;
import ru.jamsys.core.flat.template.jdbc.StatementType;

public enum JTScheduler implements JdbcRequestRepository {

    SELECT_MY_SUBSCRIBED_PLAYER("""
            SELECT
                id_player,
                max(player_about) as player_about,
                count(*) count
            FROM scheduler
            WHERE
                id_chat = ${IN.id_chat::NUMBER}
            GROUP BY id_player
            ORDER BY 1
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    SELECT_MY_SUBSCRIBED_GAMES("""
            SELECT
                *
            FROM scheduler
            WHERE
                id_chat = ${IN.id_chat::NUMBER}
                AND id_player = ${IN.id_player::NUMBER}
            ORDER BY id ASC
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    REMOVE_MY_SUBSCRIBED("""
            DELETE
            FROM scheduler
            WHERE
                id_chat = ${IN.id_chat::NUMBER}
                AND id_player = ${IN.id_player::NUMBER}
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    REMOVE_FINISH_GAME("""
            DELETE
            FROM scheduler
            WHERE
                id_game = ${IN.id_game::VARCHAR}
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    INSERT("""
            INSERT INTO scheduler (
                id_chat,
                id_player,
                id_team,
                id_game,
                time_game_start,
                game_about,
                player_about,
                test
            )
            VALUES (
                ${IN.id_chat::NUMBER},
                ${IN.id_player::NUMBER},
                ${IN.id_team::NUMBER},
                ${IN.id_game::VARCHAR},
                ${IN.time_game_start::TIMESTAMP},
                ${IN.game_about::VARCHAR},
                ${IN.player_about::VARCHAR},
                ${IN.test::VARCHAR}
            )
            ON CONFLICT DO NOTHING
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    SELECT_ACTIVE_GAME("""
            SELECT
            	id_game,
            	count(*)
            FROM scheduler
            WHERE
            	time_game_start < now()::timestamp
            GROUP BY id_game
            ORDER BY 2 DESC
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    SELECT_SUBSCRIBER_BY_PLAYER("""
           SELECT distinct on(id_chat)
               *
           FROM scheduler
           WHERE
               id_player IN (${IN.id_players::IN_ENUM_NUMBER})
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
