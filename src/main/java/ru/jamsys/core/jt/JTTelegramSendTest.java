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

//    SELECT_ONE("""
//            SELECT
//                *
//            FROM telegram_send_test
//            WHERE
//                ts_send IS NULL
//                AND ts_add < now()::timestamp
//                AND bot IN (${IN.bots::IN_ENUM_VARCHAR})
//            ORDER BY id DESC
//            LIMIT 1
//            FOR UPDATE OF telegram_send_test SKIP LOCKED
//            """, StatementType.SELECT_WITHOUT_AUTO_COMMIT),
//
//    SELECT_COUNT("""
//            SELECT
//                count(*)
//            FROM telegram_send_test
//            WHERE
//                ts_send IS NULL
//                AND ts_add < now()::timestamp
//                AND bot IN (${IN.bots::IN_ENUM_VARCHAR})
//            ORDER BY id DESC
//            """, StatementType.SELECT_WITHOUT_AUTO_COMMIT),
//
//    COMMIT("""
//            COMMIT;
//            """, StatementType.SELECT_WITHOUT_AUTO_COMMIT),
//
//    SEND_FINISH("""
//            UPDATE telegram_send_test SET
//                ts_send = now()::timestamp,
//                json = ${IN.json::VARCHAR}
//            WHERE
//                id = ${IN.id::NUMBER}
//            """, StatementType.SELECT_WITHOUT_AUTO_COMMIT),
//
//    SEND_RETRY("""
//            UPDATE telegram_send_test SET
//                ts_add = now()::timestamp + interval '1 min',
//                json = ${IN.json::VARCHAR}
//            WHERE
//                id = ${IN.id::NUMBER}
//            """, StatementType.SELECT_WITHOUT_AUTO_COMMIT),

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
                ${IN.buttons::VARCHAR},
                ${IN.ref_json::VARCHAR}
            )
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    INSERT_TS_ADD("""
            INSERT INTO telegram_send_test (
                ts_add,
                id_chat,
                bot,
                message,
                path_image,
                buttons,
                id_image,
                id_video
            )
            VALUES (
                ${IN.ts_add::TIMESTAMP},
                ${IN.id_chat::NUMBER},
                ${IN.bot::VARCHAR},
                ${IN.message::VARCHAR},
                ${IN.path_image::VARCHAR},
                ${IN.buttons::VARCHAR},
                ${IN.id_image::VARCHAR},
                ${IN.id_video::VARCHAR}
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
        String idImage;
        String idVideo;
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
