package ru.jamsys;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.flat.util.UtilListSort;
import ru.jamsys.core.flat.util.UtilNHL;
import ru.jamsys.tank.data.NHLTeamSchedule;

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

}