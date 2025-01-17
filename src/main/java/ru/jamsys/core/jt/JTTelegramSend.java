package ru.jamsys.core.jt;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.flat.template.jdbc.DataMapper;
import ru.jamsys.core.flat.template.jdbc.JdbcRequestRepository;
import ru.jamsys.core.flat.template.jdbc.JdbcTemplate;
import ru.jamsys.core.flat.template.jdbc.StatementType;

import java.math.BigDecimal;
import java.sql.Timestamp;

public enum JTTelegramSend implements JdbcRequestRepository {

    SELECT("""
            SELECT * FROM telegram_send WHERE ts_send IS NULL LIMIT 1 FOR UPDATE OF telegram_send SKIP LOCKED
            """, StatementType.SELECT_WITHOUT_AUTO_COMMIT),

    INSERT("""
            INSERT INTO telegram_send (
                id_chat,
                bot,
                message,
                path_image,
                buttons
            )
            VALUES (
                ${IN.id_chat::NUMBER},
                ${IN.bot::VARCHAR},
                ${IN.message::VARCHAR},
                ${IN.path_image::VARCHAR},
                ${IN.buttons::VARCHAR}
            )
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    SEND("""
            UPDATE telegram_send SET
                ts_send = now()::timestamp
            WHERE
                id = ${IN.id::VARCHAR}
            """, StatementType.SELECT_WITH_AUTO_COMMIT),
    ;

    @Getter
    @Setter
    public static class Row extends DataMapper<JTTeamScheduler.Row> {
        BigDecimal id;
        Timestamp tsAdd;
        BigDecimal idChat;
        String bot;
        String message;
        String pathImage;
        Timestamp tsSend;
        String buttons;
    }

    private final JdbcTemplate jdbcTemplate;

    JTTelegramSend(String sql, StatementType statementType) {
        jdbcTemplate = new JdbcTemplate(sql, statementType);
    }

    @Override
    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

}
