package ru.jamsys.core.jt;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.flat.template.jdbc.DataMapper;
import ru.jamsys.core.flat.template.jdbc.JdbcRequestRepository;
import ru.jamsys.core.flat.template.jdbc.JdbcTemplate;
import ru.jamsys.core.flat.template.jdbc.StatementType;

import java.math.BigDecimal;
import java.sql.Timestamp;

public enum JTVote implements JdbcRequestRepository {

    SELECT("""
            SELECT
                *
            FROM vote
            WHERE
               id_chat = ${IN.id_chat::NUMBER}
               AND id_game = ${IN.id_game::VARCHAR}
               AND id_player = ${IN.id_player::NUMBER}
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    INSERT("""
            INSERT INTO vote (
                id_chat,
                id_game,
                id_player,
                vote
            )
            VALUES (
                ${IN.id_chat::NUMBER},
                ${IN.id_game::VARCHAR},
                ${IN.id_player::NUMBER},
                ${IN.vote::VARCHAR}
            )
            ON CONFLICT DO NOTHING
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    SELECT_AGG("""
            SELECT
            	vote as unit,
            	count(*)
            FROM vote
            WHERE
            	id_game = ${IN.id_game::VARCHAR}
            	AND id_player = ${IN.id_player::NUMBER}
            GROUP BY vote
            """, StatementType.SELECT_WITH_AUTO_COMMIT);



    private final JdbcTemplate jdbcTemplate;

    @Getter
    @Setter
    public static class Row extends DataMapper<Row> {
        BigDecimal id;
        Timestamp tsAdd;
        BigDecimal idChat;
        String idGame;
        BigDecimal idPlayer;
        String vote;
    }

    JTVote(String sql, StatementType statementType) {
        jdbcTemplate = new JdbcTemplate(sql, statementType);
    }

    @Override
    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

}
