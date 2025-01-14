package ru.jamsys.core.jt;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.flat.template.jdbc.DataMapper;
import ru.jamsys.core.flat.template.jdbc.JdbcRequestRepository;
import ru.jamsys.core.flat.template.jdbc.JdbcTemplate;
import ru.jamsys.core.flat.template.jdbc.StatementType;
import ru.jamsys.tank.data.NHLPlayerList;

import java.math.BigDecimal;
import java.sql.Timestamp;

public enum JTPlayerSubscriber implements JdbcRequestRepository {

    SELECT("""
            SELECT
                *
            FROM player_subscriber
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    SELECT_IS_SUBSCRIBE_PLAYER("""
            SELECT
                *
            FROM player_subscriber
            WHERE
                id_chat = ${IN.id_chat::NUMBER}
                AND id_player = ${IN.id_player::NUMBER}
            ORDER BY id ASC
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    SELECT_MY_PLAYERS("""
            SELECT
                *
            FROM player_subscriber
            WHERE
                id_chat = ${IN.id_chat::NUMBER}
            ORDER BY id ASC
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    DELETE_SUBSCRIBE_PLAYER("""
            DELETE
            FROM player_subscriber
            WHERE
                id_chat = ${IN.id_chat::NUMBER}
                AND id_player = ${IN.id_player::NUMBER}
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    INSERT("""
            INSERT INTO player_subscriber (
                id_chat,
                id_player,
                id_team
            )
            VALUES (
                ${IN.id_chat::NUMBER},
                ${IN.id_player::NUMBER},
                ${IN.id_team::NUMBER}
            )
            ON CONFLICT DO NOTHING
            """, StatementType.SELECT_WITH_AUTO_COMMIT);

    @Getter
    @Setter
    public static class Row extends DataMapper<Row> {
        BigDecimal id;
        Timestamp tsAdd;
        BigDecimal idChat;
        BigDecimal idPlayer;
        BigDecimal idTeam;

        public NHLPlayerList.Player getPlayer() {
            return NHLPlayerList.findByIdStatic(getIdPlayer().toString());
        }
    }

    private final JdbcTemplate jdbcTemplate;

    JTPlayerSubscriber(String sql, StatementType statementType) {
        jdbcTemplate = new JdbcTemplate(sql, statementType);
    }

    @Override
    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

}
