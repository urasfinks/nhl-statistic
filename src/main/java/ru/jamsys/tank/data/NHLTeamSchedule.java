package ru.jamsys.tank.data;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.jamsys.core.App;
import ru.jamsys.core.flat.util.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.*;

public class NHLTeamSchedule {

    public static String getUri(String idTeam, String season) {
        return "/getNHLTeamSchedule?teamID=" + idTeam + "&season=" + season;
    }

    @SuppressWarnings("unused")
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

    @Getter
    @Setter
    public static class Instance {

        private List<Map<String, Object>> listGame;

        public Instance(List<Map<String, Object>> listGame) {
            this.listGame = listGame;
        }

        public Game getById(String idGame) {
            for (Game game : getListGameObject()) {
                if (game.getId().equals(idGame)) {
                    return game;
                }
            }
            return null;
        }

        public Instance(String json) throws Throwable {
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = UtilJson.toObject(json, Map.class);
            if (parsed.containsKey("error")) {
                throw new RuntimeException(parsed.get("error").toString());
            }
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> selector = (List<Map<String, Object>>) UtilJson.selector(parsed, "body.schedule");
            this.listGame = selector;
        }

        public List<Game> getListGameObject() {
            List<Game> result = new ArrayList<>();
            listGame.forEach(map -> result.add(new Game(map)));
            return result;
        }

        public String getGameToday(String nowDateEpoch) {
            for (Map<String, Object> game : listGame) {
                try {
                    NHLTeamSchedule.extendGameTimeZone(game);
                } catch (Exception e) {
                    App.error(e);
                }
                if (game.get("gameDateEpoch").equals(nowDateEpoch)) {
                    return game.get("gameID").toString();
                }
            }
            return null;
        }

        public Instance sort(UtilListSort.Type type) {
            List<Map<String, Object>> gameTimeEpoch = UtilListSort.sort(
                    getListGame(),
                    type,
                    stringObjectMap -> new BigDecimal(stringObjectMap.get("gameTime_epoch").toString()).longValue()
            );
            return new Instance(gameTimeEpoch);
        }

        public Instance extend() {
            List<Map<String, Object>> result = new ArrayList<>();
            getListGame().forEach(stringObjectMap -> result.add(new Game(stringObjectMap).getData()));
            return new Instance(result);
        }

        public Instance getFutureGame() {
            long currentTimestamp = UtilDate.getTimestamp();
            List<Map<String, Object>> gameTimeEpoch = getListGame().stream().filter(game -> {
                // Сейчас 14:26
                // Игра началась в 14:00
                // timestamp игры меньше чем сейчас
                // изначально планировал, что будем брать все игры у которых timestamp больше чем сейчас
                // Но тогда мы не возьмём игру, которая в процессе, поэтому сравнивать будем за вычитом времени игры
                long gameStartTimestamp = new BigDecimal(game.get("gameTime_epoch").toString()).longValue();
                // 5 часов просто накинул
                return gameStartTimestamp > (currentTimestamp - 5 * 60 * 60);
            }).toList();
            return new Instance(gameTimeEpoch);
        }

        public Instance getScheduledAndLive() {
            return new Instance(getListGame().stream().filter(game -> {
                String gameStatus = game.get("gameStatus").toString(); // https://www.tank01.com/Guides_Game_Status_Code_NHL.html
                return game.containsKey("gameStatus") &&
                        (
                                gameStatus.equals("Scheduled")
                                        || gameStatus.equals("Live - In Progress")
                        );
            }).toList());
        }

        public Game getGame(int index) {
            return new Game(listGame.get(index));
        }

        public Set<String> getIdGame() {
            Set<String> result = new LinkedHashSet<>();
            listGame.forEach(stringObjectMap -> result.add(stringObjectMap.get("gameID").toString()));
            return result;
        }

    }

    @Getter
    @ToString
    public static class Game {

        private final Map<String, Object> data;

        public Game(Map<String, Object> data) {
            this.data = data;
            try {
                NHLTeamSchedule.extendGameTimeZone(data);
                data.put("homeTeam", NHLTeams.teams.getByAbv(data.get("home").toString()).getAbout());
                data.put("awayTeam", NHLTeams.teams.getByAbv(data.get("away").toString()).getAbout());
                data.put("about", data.get("homeTeam") + " vs " + data.get("awayTeam"));
            } catch (Throwable e) {
                App.error(e);
            }
        }

        public String getId() {
            return data.get("gameID").toString();
        }

        public String getMoscowDate() {
            return UtilDate.timestampFormatUTC(
                    new BigDecimal(data.get("gameTime_epoch").toString()).longValue() + 3 * 60 * 60,
                    "dd.MM.yyyy HH:mm"
            );
        }

        public String getMoscowDate(String format) {
            return UtilDate.timestampFormatUTC(
                    new BigDecimal(data.get("gameTime_epoch").toString()).longValue() + 3 * 60 * 60,
                    format
            );
        }

        public String getGameTimeFormat() {
            return String.format(
                    "%s (UTC%s)",
                    NHLTeamSchedule.getGameLocalTime(data, "dd.MM.yyyy HH:mm"),
                    data.get("timeZone")
            );
        }

        public String getGameAbout() {
            return String.format("%s %s vs %s",
                    getGameTimeFormat(),
                    data.get("homeTeam"),
                    data.get("awayTeam")
            );
        }

        public String toggleTeam(String team) {
            return getData().get(data.get("away").equals(team) ? "homeTeam" : "awayTeam").toString();
        }

    }

}
