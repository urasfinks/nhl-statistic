package ru.jamsys.core.jt;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.flat.template.jdbc.DataMapper;
import ru.jamsys.core.flat.template.jdbc.JdbcRequestRepository;
import ru.jamsys.core.flat.template.jdbc.JdbcTemplate;
import ru.jamsys.core.flat.template.jdbc.StatementType;

import java.math.BigDecimal;
import java.sql.Timestamp;

public enum JTTelegramSendTest implements JdbcRequestRepository {

    SELECT("""
            SELECT * FROM telegram_send_test WHERE ts_send IS NULL
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    INSERT("""
            INSERT INTO telegram_send_test (
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

    INSERT_TS_ADD("""
            INSERT INTO telegram_send_test (
                ts_add,
                id_chat,
                bot,
                message,
                path_image,
                buttons
            )
            VALUES (
                ${IN.ts_add::TIMESTAMP},
                ${IN.id_chat::NUMBER},
                ${IN.bot::VARCHAR},
                ${IN.message::VARCHAR},
                ${IN.path_image::VARCHAR},
                ${IN.buttons::VARCHAR}
            )
            """, StatementType.SELECT_WITH_AUTO_COMMIT);

    @Getter
    @Setter
    public static class Row extends DataMapper<Row> {
        BigDecimal id;
        Timestamp tsAdd;
        BigDecimal idChat;
        String bot;
        String message;
        String pathImage;
        Timestamp tsSend;
        String buttons;
        String json;
    }

    private final JdbcTemplate jdbcTemplate;

    JTTelegramSendTest(String sql, StatementType statementType) {
        jdbcTemplate = new JdbcTemplate(sql, statementType);
    }

    @Override
    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

}
