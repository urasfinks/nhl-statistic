package ru.jamsys;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.flat.util.UtilListSort;
import ru.jamsys.core.flat.util.UtilNHL;
import ru.jamsys.tank.data.NHLTeamSchedule;

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
        System.out.println(Calendar.getInstance().get(Calendar.YEAR));
        long timestampMills = Long.parseLong("1733274000000");
        LocalDateTime timestamp = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestampMills), ZoneId.systemDefault());
        System.out.println(timestamp);
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
                .getFutureGame()
                .sort(UtilListSort.Type.ASC)
                .getListGame();
        Assertions.assertEquals("20241229_DAL@CHI", sortGameByTime.getFirst().get("gameID"));
    }

    @Test
    void getMoscowGameDate() throws Throwable {
        NHLTeamSchedule.Game game = new NHLTeamSchedule.Instance(NHLTeamSchedule.getExample())
                .getScheduledAndLive()
                .getFutureGame()
                .sort(UtilListSort.Type.ASC)
                .getGame(0).extend();
        Assertions.assertEquals("30.12.2024 04:30", game.getMoscowDate());
    }

    @Test
    void toggle() throws Throwable {
        NHLTeamSchedule.Game game = new NHLTeamSchedule.Instance(NHLTeamSchedule.getExample())
                .getScheduledAndLive()
                .getFutureGame()
                .sort(UtilListSort.Type.ASC)
                .getGame(0);
        Assertions.assertEquals("Dallas Stars (DAL)", game.toggleTeam("CHI"));
    }

    @Test
    void test() throws Throwable {
        Map<String, Object> game = new NHLTeamSchedule.Instance(NHLTeamSchedule.getExample())
                .getScheduledAndLive()
                .extend()
                .getListGame()
                .getFirst();
        Assertions.assertEquals("-05:00", game.get("timeZone"));
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
        System.out.println(UtilJson.toStringPretty(game, "{}"));
    }

    @Test
    void nhlSeason() throws ParseException {
        Assertions.assertEquals(2025, UtilNHL.getActiveSeasonOrNext("2024-12-20", "yyyy-MM-dd"));
        Assertions.assertEquals(2025, UtilNHL.getActiveSeasonOrNext("2024-11-20", "yyyy-MM-dd"));
        Assertions.assertEquals(2025, UtilNHL.getActiveSeasonOrNext("2024-10-20", "yyyy-MM-dd"));
        Assertions.assertEquals(2025, UtilNHL.getActiveSeasonOrNext("2024-10-01", "yyyy-MM-dd"));

        Assertions.assertNull(UtilNHL.getActiveSeasonOrNext("2024-09-30", "yyyy-MM-dd"));
        Assertions.assertNull(UtilNHL.getActiveSeasonOrNext("2024-05-01", "yyyy-MM-dd"));

        Assertions.assertEquals(2024, UtilNHL.getActiveSeasonOrNext("2024-04-30", "yyyy-MM-dd"));
        Assertions.assertEquals(2024, UtilNHL.getActiveSeasonOrNext("2024-01-01", "yyyy-MM-dd"));

        Assertions.assertNull(UtilNHL.getActiveSeasonOrNext("2023-05-01", "yyyy-MM-dd"));

        Assertions.assertEquals(2023, UtilNHL.getActiveSeasonOrNext("2023-04-30", "yyyy-MM-dd"));
    }

    @Test
    void seasonFormat() {
        Assertions.assertEquals("Сезон не определён", UtilNHL.seasonFormat(null));
        Assertions.assertEquals("Сезон не определён", UtilNHL.seasonFormat(1));
        Assertions.assertEquals("Сезон не определён", UtilNHL.seasonFormat(11));
        Assertions.assertEquals("Сезон не определён", UtilNHL.seasonFormat(111));
        Assertions.assertEquals("1110/11", UtilNHL.seasonFormat(1111));
        Assertions.assertEquals("2024/25", UtilNHL.seasonFormat(2025));
        Assertions.assertEquals("2023/24", UtilNHL.seasonFormat(2024));
    }

    @Test
    void getGameToday() throws Throwable {
        NHLTeamSchedule.Instance instance = new NHLTeamSchedule.Instance(NHLTeamSchedule.getExample_18_2025());
        Assertions.assertEquals("20241212_LA@NJ", instance.getGameToday("20241213"));
    }

}