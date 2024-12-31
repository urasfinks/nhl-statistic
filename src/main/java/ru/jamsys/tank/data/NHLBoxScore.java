package ru.jamsys.tank.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilFileResource;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.telegram.GameEventData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NHLBoxScore {

    public static String getUri(String idGame) {
        try {
            return "/getNHLBoxScore?gameID=" + Util.urlEncode(idGame);
        } catch (Exception e) {
            throw new ForwardException(e);
        }
    }

    public static String getExample() throws IOException {
        return UtilFileResource.getAsString("example/getNHLBoxScore.json");
    }

    public static String getExampleChange() throws IOException {
        return UtilFileResource.getAsString("example/getNHLBoxScore_change.json");
    }

    public static String getExample4() throws IOException {
        return UtilFileResource.getAsString("example/getNHLBoxScore4.json");
    }

    @SuppressWarnings("unused")
    public static String getExampleEmptyScore() throws IOException {
        return UtilFileResource.getAsString("example/getNHLBoxScore_empty_scoring.json");
    }

    public static String getExample5() throws IOException {
        return UtilFileResource.getAsString("example/getNHLBoxScore5.json");
    }

    public static String getExample6() throws IOException {
        return UtilFileResource.getAsString("example/getNHLBoxScore6.json");
    }

    public static String getExampleError() throws IOException {
        return UtilFileResource.getAsString("example/getNHLBoxScore_error.json");
    }

    public static Map<String, List<GameEventData>> getEvent(String lastJson, String currentJson) throws Throwable {
        Map<String, List<GameEventData>> result = new HashMap<>();
        Instance lastInstance = new Instance(lastJson);
        Instance currentInstance = new Instance(currentJson);
        lastInstance.getPlayerStats().forEach((idPlayer, lastStat) -> {
            Map<String, Object> currentStat = currentInstance.getPlayerStats().get(idPlayer);
            // Бывает такое что тупо нет goal
            if (lastStat.containsKey("goals") && currentStat.containsKey("goals")
                    && !lastStat.get("goals").equals(currentStat.get("goals"))) {
                int lastGoals = Integer.parseInt(lastStat.get("goals").toString());
                int currentGoals = Integer.parseInt(currentStat.get("goals").toString());
                int diff = currentGoals - lastGoals;
                List<Map<String, Object>> listGoal = (diff > 0 ? currentInstance : lastInstance).getPlayer(idPlayer).getListGoal();
                getLastNElements(listGoal, Math.abs(diff)).forEach(
                        map -> result.computeIfAbsent(idPlayer, _ -> new ArrayList<>()).add(new GameEventData()
                                .setAction(diff > 0 ? GameEventData.Action.GOAL : GameEventData.Action.CANCEL)
                                .setTeamsScore(currentInstance.getScoreHome())
                                .setGameName(currentInstance.getAboutHome())
                                .setScoredGoal(currentGoals)
                                .setTime(map.get("scoreTime") + " " + periodExpandRu(map.get("period").toString()))
                                .setPlayerName(currentStat.get("longName").toString())
                        )
                );
            }
        });
        return result;
    }

    public static boolean isFinish(String json) throws Throwable {
        if (json == null || json.isEmpty()) { //Так как в БД может быть ничего
            return false;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = UtilJson.toObject(json, Map.class);
        if (parsed.containsKey("error")) {
            throw new RuntimeException(parsed.get("error").toString());
        }
        String gameStatusCode = (String) UtilJson.selector(parsed, "body.gameStatusCode");
        return Integer.parseInt(gameStatusCode) == 2;
    }

    public static String periodExpandRu(String period) {
        return periodExpandEn(period)
                .replaceAll("undefined period", "неизвестный период")
                .replaceAll("1st period", "1-й период")
                .replaceAll("2nd period", "2-й период")
                .replaceAll("3rd period", "3-й период")
                .replaceAll("overtime", "доп. время");
    }

    public static String periodExpandEn(String period) {
        if (period == null || period.isEmpty()) {
            return "undefined period";
        } else if (period.equals("1P")) {
            return "1st period";
        } else if (period.equals("2P")) {
            return "2nd period";
        } else if (period.equals("3P")) {
            return "3rd period";
        } else if (period.equals("OT")) {
            return "overtime";
        } else {
            return period;
        }
    }

    public static Map<String, Object> getPlayerStat(String json, String idPlayer) {
        @SuppressWarnings("unchecked")
        Map<String, Object> res = (Map<String, Object>) UtilJson.selector(json, "$.body.playerStats." + idPlayer);
        return res;
    }

    @Getter
    public static class Instance {

        @JsonIgnore
        final private List<Map<String, Object>> scoringPlays;

        @JsonIgnore
        final private Map<String, Map<String, Object>> playerStats;

        final private Map<String, Integer> scoreMap = new HashMap<>();

        final String scoreHome;
        final String aboutHome;

        public Instance(String json) throws Throwable {
            if (json == null || json.isEmpty()) { //Так как в БД может быть ничего
                throw new RuntimeException("json empty");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) UtilJson.toObject(json, Map.class).get("body");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> scoringPlays = (List<Map<String, Object>>) body.get("scoringPlays");
            this.scoringPlays = scoringPlays;

            @SuppressWarnings("unchecked")
            Map<String, Map<String, Object>> playerStats = (Map<String, Map<String, Object>>) body.get("playerStats");
            this.playerStats = playerStats;

            NHLTeams.Team teamHome = NHLTeams.teams.getById(body.get("teamIDHome").toString());
            NHLTeams.Team teamAway = NHLTeams.teams.getById(body.get("teamIDAway").toString());

            scoreMap.put(teamHome.getAbv(), Integer.parseInt(body.get("homeTotal").toString()));
            scoreMap.put(teamAway.getAbv(), Integer.parseInt(body.get("awayTotal").toString()));

            scoreHome = getScore(teamHome.getAbv());
            aboutHome = getAbout(teamHome.getAbv());
        }

        public String getAbout(String firstTeamAbv) {
            StringBuilder sb = new StringBuilder();
            ArrayList<String> strings = new ArrayList<>(scoreMap.keySet());
            strings.remove(firstTeamAbv);
            String lastTeamAbv = strings.getLast();
            sb.append(NHLTeams.teams.getByAbv(firstTeamAbv).getAbout())
                    .append(" 🆚 ")
                    .append(NHLTeams.teams.getByAbv(lastTeamAbv).getAbout())
            ;
            return sb.toString();
        }

        public String getScore(String firstTeamAbv) {
            StringBuilder sb = new StringBuilder();
            ArrayList<String> strings = new ArrayList<>(scoreMap.keySet());
            strings.remove(firstTeamAbv);
            String lastTeamAbv = strings.getLast();
            sb.append(NHLTeams.teams.getByAbv(firstTeamAbv).getAbout())
                    .append(" ")
                    .append(scoreMap.get(firstTeamAbv))
                    .append(" - ")
                    .append(scoreMap.get(lastTeamAbv))
                    .append(" ")
                    .append(NHLTeams.teams.getByAbv(lastTeamAbv).getAbout())
            ;
            return sb.toString();
        }

        public Player getPlayer(String idPlayer) {
            if (playerStats.containsKey(idPlayer)) {
                Player player = new Player(playerStats.get(idPlayer));
                scoringPlays.forEach(map -> {
                    if (map.containsKey("goal")) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> goal = (Map<String, Object>) map.get("goal");
                        if (goal.containsKey("playerID") && goal.get("playerID").equals(idPlayer)) {
                            player.getListGoal().add(map);
                        }
                    }
                });
                return player;
            }
            return null;
        }
    }

    @Getter
    @Setter
    public static class Player {

        final private List<Map<String, Object>> listGoal = new ArrayList<>();

        final private Map<String, Object> stat;

        public Player(Map<String, Object> stat) {
            this.stat = stat;
        }

    }

    public static <T> List<T> getLastNElements(List<T> list, int n) {
        if (list == null || n <= 0) {
            return List.of(); // Возвращаем пустой список, если входные данные некорректны
        }

        // Если n больше длины списка, возвращаем весь список
        int fromIndex = Math.max(0, list.size() - n);
        return list.subList(fromIndex, list.size());
    }

}
