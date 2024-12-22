package ru.jamsys.tank.data;

import ru.jamsys.core.App;
import ru.jamsys.core.flat.util.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class NHLTeamSchedule {

    public static String getUri(String idTeam, String season) {
        return "/getNHLTeamSchedule?teamID=" + idTeam + "&season=" + season;
    }

    public static String getExample_18_2024() throws IOException {
        return UtilFileResource.getAsString("example/NJ_2024.json");
    }

    public static String getExample_18_2025() throws IOException {
        return UtilFileResource.getAsString("example/NJ_2025.json");
    }

    public static String getExample_WSH_2024() throws IOException {
        return UtilFileResource.getAsString("example/WSH_2024.json");
    }

    public static String getExample_WSH_2025() throws IOException {
        return UtilFileResource.getAsString("example/WSH_2025.json");
    }

    public static String getExample() throws IOException {
        return UtilFileResource.getAsString("example/getNHLTeamSchedule.json");
    }

    public static void extendGameTimeZone(Map<String, Object> game) throws Exception {
        String localTimeGame = NHLTeamSchedule.getGameLocalTime(game, "yyyy-MM-dd'T'HH:mm:ss");
        String realTimeGameUtc = UtilDate.timestampFormatUTC(
                new BigDecimal(game.get("gameTime_epoch").toString()).longValue(),
                "yyyy-MM-dd'T'HH:mm:ss"
        );
        // Тут нам уже не важно локальную зону, нам просто нужна общая точка отсчёта и локальная вполне подходит
        long timestampLocalGame = UtilDate.getTimestamp(localTimeGame, "yyyy-MM-dd'T'HH:mm:ss");
        long timestampRealGame = UtilDate.getTimestamp(realTimeGameUtc, "yyyy-MM-dd'T'HH:mm:ss");
        String zone = (timestampLocalGame - timestampRealGame < 0 ? "-" : "+") + LocalTime.MIN.plusSeconds(Math.abs(timestampLocalGame - timestampRealGame)).toString();
        game.put("timeZone", zone);
        game.put("gameDateEpoch", UtilDate.timestampFormatUTC(new BigDecimal(game.get("gameTime_epoch").toString()).longValue(), "yyyyMMdd"));
        game.put("gameDateTime", localTimeGame);
        game.put("gameDateTimeEpoch", realTimeGameUtc);
    }

    public static String getGameLocalTime(Map<String, Object> game, String format) {
        String gameDate = game.get("gameDate").toString();
        String gameTime = game.get("gameTime").toString();
        int hourSec = Integer.parseInt(gameTime.substring(0, gameTime.indexOf(":"))) * 60 * 60;
        int offsetHourSec = gameTime.endsWith("p") ? (12 * 60 * 60) : 0;
        int minSec = Integer.parseInt(Util.readUntil(gameTime.substring(gameTime.indexOf(":") + 1), Util::isNumeric)) * 60;
        long dateSec = UtilDate.getTimestamp(gameDate, "yyyyMMdd", 0);
        dateSec += (hourSec + offsetHourSec + minSec);
        return UtilDate.timestampFormat(dateSec, format);
    }

    public static String getGameTimeFormat(Map<String, Object> map) {
        return String.format(
                "%s (UTC%s)",
                NHLTeamSchedule.getGameLocalTime(map, "dd/MM/yyyy HH:mm"),
                map.get("timeZone")
        );
    }

    public static String getGameAbout(Map<String, Object> map) {
        return String.format("%s %s vs %s",
                getGameTimeFormat(map),
                map.get("homeTeam"),
                map.get("awayTeam")
        );
    }

    public static List<Map<String, Object>> parseGameRaw(String json) throws Throwable {
        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = UtilJson.toObject(json, Map.class);
        if (parsed.containsKey("error")) {
            throw new RuntimeException(parsed.get("error").toString());
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> selector = (List<Map<String, Object>>) UtilJson.selector(parsed, "body.schedule");
        return selector;
    }

    public static List<Map<String, Object>> parseGameScheduledAndLive(String json) throws Throwable {
        Map<String, Object> teams = NHLTeams.getTeams();
        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = UtilJson.toObject(json, Map.class);
        if (parsed.containsKey("error")) {
            throw new RuntimeException(parsed.get("error").toString());
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> selector = (List<Map<String, Object>>) UtilJson.selector(parsed, "body.schedule");
        List<Map<String, Object>> result = new ArrayList<>();

        selector.forEach(game -> {
            String gameStatus = game.get("gameStatus").toString(); // https://www.tank01.com/Guides_Game_Status_Code_NHL.html
            if (game.containsKey("gameStatus") &&
                    (
                            gameStatus.equals("Scheduled")
                                    || gameStatus.equals("Live - In Progress")
                    )
            ) {
                game.put("homeTeam", teams.get(game.get("home")) + " (" + game.get("home") + ")");
                game.put("awayTeam", teams.get(game.get("away")) + " (" + game.get("away") + ")");
                game.put("about", game.get("homeTeam") + " vs " + game.get("awayTeam"));
                try {
                    NHLTeamSchedule.extendGameTimeZone(game);
                } catch (Exception e) {
                    App.error(e);
                }
                result.add(game);
            }
        });
        return result;
    }

    public static List<Map<String, Object>> getGameSortAndFilterByTime(List<Map<String, Object>> listGame) {
        long currentTimestamp = UtilDate.getTimestamp();
        return UtilListSort.sort(
                listGame.stream().filter(game -> {
                    // Сейчас 14:26
                    // Игра началась в 14:00
                    // timestamp игры меньше чем сейчас
                    // изначально планировал, что будем брать все игры у которых timestamp больше чем сейчас
                    // Но тогда мы не возьмём игру, которая в процессе, поэтому сравнивать будем за вычитом времени игры
                    long gameStartTimestamp = new BigDecimal(game.get("gameTime_epoch").toString()).longValue();
                    // 5 часов просто накинул
                    return gameStartTimestamp > (currentTimestamp - 5 * 60 * 60);
                }).toList(),
                UtilListSort.Type.ASC,
                stringObjectMap -> new BigDecimal(stringObjectMap.get("gameTime_epoch").toString()).longValue()
        );
    }

    // Метод для определения сезона по дате
    public static Integer getActiveSeasonOrNext() {
        return getActiveSeasonOrNext(LocalDate.ofInstant(new Date().toInstant(), ZoneId.systemDefault()));
    }

    public static Integer getActiveSeasonOrNext(String date, String format) throws ParseException {
        DateFormat dateFormat = new SimpleDateFormat(format);
        Date input = dateFormat.parse(date);
        return getActiveSeasonOrNext(LocalDate.ofInstant(input.toInstant(), ZoneId.systemDefault()));
    }

    public static Integer getActiveSeasonOrNext(LocalDate date) {
        int year = date.getYear();
        if (date.getMonthValue() <= Month.APRIL.getValue()) {
            return year;
        } else if (date.getMonthValue() >= Month.OCTOBER.getValue()) {
            return year + 1;
        } else { // Потому что возможно ещё не сформировано расписание, нет смысла раньше времени
            return null;
        }
    }

}
