package ru.jamsys;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.flat.util.UtilListSort;
import ru.jamsys.core.flat.util.UtilNHL;
import ru.jamsys.tank.data.NHLTeamSchedule;
import ru.jamsys.telegram.TelegramBotHttpSender;

import java.math.BigDecimal;
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

class NHLTeamScheduleTest {

    @Test
    void getYear() {
        Util.logConsole(getClass(), String.valueOf(Calendar.getInstance().get(Calendar.YEAR)));
        long timestampMills = Long.parseLong("1733274000000");
        LocalDateTime timestamp = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestampMills), ZoneId.systemDefault());
        Util.logConsole(getClass(), String.valueOf(timestamp));
    }

    @Test
    void parseGameScheduledAndLive() throws Throwable {
        List<Map<String, Object>> game = new NHLTeamSchedule.Instance(NHLTeamSchedule.getExample())
                .getScheduledAndLive()
                .sort(UtilListSort.Type.ASC)
                .extend()
                .getListGame();

        Assertions.assertEquals(60, game.size());
        Assertions.assertEquals("20241129_CHI@MIN", game.getFirst().get("gameID"));
        Assertions.assertEquals("Minnesota Wild (MIN)", game.getFirst().get("homeTeam"));
    }

    @Test
    void getGameSortAndFilterByTime() throws Throwable {
        List<Map<String, Object>> sortGameByTime = new NHLTeamSchedule.Instance(NHLTeamSchedule.getExample())
                .getScheduledAndLive()
                .sort(UtilListSort.Type.ASC)
                .getListGame();
        Assertions.assertEquals("20241129_CHI@MIN", sortGameByTime.getFirst().get("gameID"));
    }

    @Test
    void getMoscowGameDate() throws Throwable {
        NHLTeamSchedule.Game game = new NHLTeamSchedule.Instance(NHLTeamSchedule.getExample())
                .getScheduledAndLive()
                .sort(UtilListSort.Type.ASC)
                .getGame(0);
        Assertions.assertEquals("29.11.2024 22:00", game.getMoscowDate());
    }

    @Test
    void toggle() throws Throwable {
        NHLTeamSchedule.Game game = new NHLTeamSchedule.Instance(NHLTeamSchedule.getExample())
                .getScheduledAndLive()
                .sort(UtilListSort.Type.ASC)
                .getGame(0);
        Assertions.assertEquals("Minnesota Wild (MIN)", game.toggleTeam("CHI"));
    }

    @Test
    void test() throws Throwable {
        NHLTeamSchedule.Instance game = new NHLTeamSchedule.Instance(NHLTeamSchedule.getExample())
                .getScheduledAndLive()
                .extend();
        Assertions.assertEquals("-05:00", game.getListGame().getFirst().get("timeZone"));
        Assertions.assertEquals("7", game.getIdTeam());

    }

    @Test
    void test2() throws Throwable {
        String data = """
                {
                  "gameID" : "20241210_COL@PIT",
                  "seasonType" : "Regular Season",
                  "away" : "COL",
                  "gameTime" : "7:00p",
                  "teamIDHome" : "23",
                  "gameDate" : "20241210",
                  "gameStatus" : "Scheduled",
                  "gameTime_epoch" : "1733875200.0",
                  "teamIDAway" : "8",
                  "home" : "PIT",
                  "gameStatusCode" : "0",
                  "homeTeam" : "Pittsburgh Penguins (PIT)",
                  "awayTeam" : "Colorado Avalanche (COL)",
                  "about" : "Pittsburgh Penguins (PIT) vs Colorado Avalanche (COL)"
                }
                """;
        Map<String, Object> game = UtilJson.getMapOrThrow(data);
        NHLTeamSchedule.extendGameTimeZone(game);
    }

    @Test
    void getGameToday() throws Throwable {
        NHLTeamSchedule.Instance instance = new NHLTeamSchedule.Instance(NHLTeamSchedule.getExample_18_2025());
        Assertions.assertEquals("20241212_LA@NJ", instance.getIdGameToday("20241213"));
    }

    @Test
    void testDate() throws ParseException {
        Assertions.assertEquals("24 января в 03:00 (МСК)", UtilNHL.formatDate(new BigDecimal("1737676800.0").longValue()));
    }

    @Test
    void testGetId(){
        String fileId = TelegramBotHttpSender.getFilePhotoId("""
                 {
                    "message_id" : 4849,
                    "from" : {
                      "id" : 290029195,
                      "first_name" : "Юра Мухин",
                      "is_bot" : false,
                      "username" : "urasfinks",
                      "language_code" : "ru",
                      "is_premium" : true
                    },
                    "date" : 1738406813,
                    "chat" : {
                      "id" : 290029195,
                      "type" : "private",
                      "first_name" : "Юра Мухин",
                      "username" : "urasfinks"
                    },
                    "photo" : [
                      {
                        "file_id" : "AgACAgIAAxkBAAIS6Wed-TkCuCanrYFe3p4nfVKFrJHdAAIg7zEbui3wSBrhvpLj7DInAQADAgADcwADNgQ",
                        "file_unique_id" : "AQADIO8xG7ot8Eh4",
                        "width" : 90,
                        "height" : 90,
                        "file_size" : 928
                      },
                      {
                        "file_id" : "AgACAgIAAxkBAAIS6Wed-TkCuCanrYFe3p4nfVKFrJHdAAIg7zEbui3wSBrhvpLj7DInAQADAgADbQADNgQ",
                        "file_unique_id" : "AQADIO8xG7ot8Ehy",
                        "width" : 320,
                        "height" : 320,
                        "file_size" : 10305
                      },
                      {
                        "file_id" : "AgACAgIAAxkBAAIS6Wed-TkCuCanrYFe3p4nfVKFrJHdAAIg7zEbui3wSBrhvpLj7DInAQADAgADeQADNgQ",
                        "file_unique_id" : "AQADIO8xG7ot8Eh-",
                        "width" : 1000,
                        "height" : 1000,
                        "file_size" : 34481
                      },
                      {
                        "file_id" : "AgACAgIAAxkBAAIS6Wed-TkCuCanrYFe3p4nfVKFrJHdAAIg7zEbui3wSBrhvpLj7DInAQADAgADeAADNgQ",
                        "file_unique_id" : "AQADIO8xG7ot8Eh9",
                        "width" : 800,
                        "height" : 800,
                        "file_size" : 39523
                      }
                    ]
                  }""");
        Assertions.assertEquals("AgACAgIAAxkBAAIS6Wed-TkCuCanrYFe3p4nfVKFrJHdAAIg7zEbui3wSBrhvpLj7DInAQADAgADeQADNgQ", fileId);

        String filePhotoId = TelegramBotHttpSender.getFilePhotoId("{\"ok\":true,\"result\":{\"message_id\":9597,\"from\":{\"id\":7770107380,\"is_bot\":true,\"first_name\":\"ovi_goals_bot_test\",\"username\":\"test_ovi_goals_bot\"},\"chat\":{\"id\":290029195,\"first_name\":\"\\u042e\\u0440\\u0430 \\u041c\\u0443\\u0445\\u0438\\u043d\",\"username\":\"urasfinks\",\"type\":\"private\"},\"date\":1738779970,\"photo\":[{\"file_id\":\"AgACAgIAAxkDAAIlWGejqwgft29TQDh8id_CYU_6uUuXAAJy8jEbtNghSRIg0GTETIDWAQADAgADcwADNgQ\",\"file_unique_id\":\"AQADcvIxG7TYIUl4\",\"file_size\":750,\"width\":90,\"height\":67},{\"file_id\":\"AgACAgIAAxkDAAIlWGejqwgft29TQDh8id_CYU_6uUuXAAJy8jEbtNghSRIg0GTETIDWAQADAgADbQADNgQ\",\"file_unique_id\":\"AQADcvIxG7TYIUly\",\"file_size\":10586,\"width\":320,\"height\":240},{\"file_id\":\"AgACAgIAAxkDAAIlWGejqwgft29TQDh8id_CYU_6uUuXAAJy8jEbtNghSRIg0GTETIDWAQADAgADeAADNgQ\",\"file_unique_id\":\"AQADcvIxG7TYIUl9\",\"file_size\":37376,\"width\":800,\"height\":600}]}}\n");
        Assertions.assertEquals("AgACAgIAAxkDAAIlWGejqwgft29TQDh8id_CYU_6uUuXAAJy8jEbtNghSRIg0GTETIDWAQADAgADeAADNgQ", filePhotoId);
    }

}