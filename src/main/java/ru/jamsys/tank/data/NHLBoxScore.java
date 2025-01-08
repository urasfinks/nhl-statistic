package ru.jamsys.tank.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import ru.jamsys.core.App;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.flat.util.*;
import ru.jamsys.telegram.GameEventData;

import java.io.IOException;
import java.util.*;

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

    public static String getExampleTorWSH() throws IOException {
        return UtilFileResource.getAsString("example/boxTOR_WSH.json");
    }

    public static String getExampleTorWSH_withoutOviStat() throws IOException {
        return UtilFileResource.getAsString("example/boxTOR_WSH_withoutOviStat.json");
    }

    public static String getExampleTorWSH_live() throws IOException {
        return UtilFileResource.getAsString("example/boxTOR_WSH_live.json");
    }

    public static String getExampleTorWSH_liveChange() throws IOException {
        return UtilFileResource.getAsString("example/boxTOR_WSH_live_change.json");
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

    public static String getExampleError() throws IOException {
        return UtilFileResource.getAsString("example/getNHLBoxScore_error.json");
    }

    public static String getExample_20250104_BOS_TOR() throws IOException {
        return UtilFileResource.getAsString("example/20250104_BOS_TOR.json");
    }

    public static String getExample_20250104_BOS_TOR_2() throws IOException {
        return UtilFileResource.getAsString("example/20250104_BOS_TOR_2.json");
    }

    // Получить события по игрокам на разности снимков статистики
    public static Map<String, List<GameEventData>> getEvent(String lastJson, String currentJson) throws Throwable {
        Map<String, List<GameEventData>> result = new HashMap<>(); //key: idPlayer; value: list GameEventData
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
                List<Map<String, Object>> listGoal = (diff > 0 ? currentInstance : lastInstance)
                        .getPlayer(idPlayer)
                        .getSortByTimeListGoal(UtilListSort.Type.ASC);
                getLastNElements(listGoal, Math.abs(diff)).forEach(map -> result
                        .computeIfAbsent(idPlayer, _ -> new ArrayList<>())
                        .add(new GameEventData(
                                        diff > 0 ? GameEventData.Action.GOAL : GameEventData.Action.CANCEL,
                                        currentInstance.getAboutGame(),
                                        currentInstance.getScoreGame(),
                                        currentInstance.getPlayer(idPlayer).getLongName(),
                                        map.get("scoreTime") + ", " + periodExpandRu(map.get("period").toString())
                                )
                                        .setAction(diff > 0 ? GameEventData.Action.GOAL : GameEventData.Action.CANCEL)
                                        .setScoredGoal(currentGoals)
                                        .setScoredLastSeason(UtilNHL.isOvi(idPlayer)
                                                ? UtilNHL.getOviScoreLastSeason()
                                                : 0
                                        )
                        )
                );
            }
        });
        return result;
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

        final private int gameStatusCode;

        final String scoreGame;
        final String aboutGame;

        public Instance(String json) throws Throwable {
            if (json == null || json.isEmpty()) { //Так как в БД может быть ничего
                throw new RuntimeException("json empty");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> object = UtilJson.toObject(json, Map.class);
            if (object.containsKey("error")) {
                throw new RuntimeException(object.get("error").toString());
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) object.get("body");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> scoringPlays = (List<Map<String, Object>>) body.get("scoringPlays");
            this.scoringPlays = scoringPlays != null ? scoringPlays : new ArrayList<>();

            @SuppressWarnings("unchecked")
            Map<String, Map<String, Object>> playerStats = (Map<String, Map<String, Object>>) body.get("playerStats");
            this.playerStats = playerStats != null ? playerStats : new HashMap<>();

            NHLTeams.Team teamHome = NHLTeams.teams.getById(body.get("teamIDHome").toString());
            NHLTeams.Team teamAway = NHLTeams.teams.getById(body.get("teamIDAway").toString());

            scoreMap.put(teamHome.getAbv(), Integer.parseInt(body.getOrDefault("homeTotal", "0").toString()));
            scoreMap.put(teamAway.getAbv(), Integer.parseInt(body.getOrDefault("awayTotal", "0").toString()));

            scoreGame = getScoreGame(teamAway.getAbv());
            aboutGame = getAboutGame(teamAway.getAbv());

            gameStatusCode = Integer.parseInt(body.getOrDefault("gameStatusCode", "-1").toString());
        }

        public boolean isFinish() {
            List<String> listAbv = scoreMap.keySet().stream().toList();
            if (scoreMap.get(listAbv.getFirst()).equals(scoreMap.get(listAbv.getLast()))) {
                return false;
            }
            return gameStatusCode == 2;
        }

        public List<String> getListIdPlayer(Set<String> listIdPlayer) {
            if (listIdPlayer == null) {
                return playerStats.keySet().stream().toList();
            }
            HashSet<String> result = new HashSet<>(listIdPlayer);
            result.addAll(playerStats.keySet());
            return result.stream().toList();
        }

        public String getAboutGame(String firstTeamAbv) {
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

        public String getScoreGame(String firstTeamAbv) {
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
                        // Мы должны сразу откинуть SO потому что время гола берётся из listGoal
                        // Если предположим БОТ протупит и выполнит генерацию событий, после буллита
                        // Будут голы у игрока и мы возьмём с конца scoringPlays и нарвёися на SO
                        // А это не корректно, SO вообще за голы не считаются и scoreTime у них нет
                        if (
                                goal.containsKey("playerID") && goal.get("playerID").equals(idPlayer)
                                        && !"SO".equals(map.get("period"))
                        ) {
                            player.addGoal(map);
                        }
                    }
                });
                return player;
            }
            return null;
        }

        public boolean isPenaltyShot() {
            for (Map<String, Object> map : scoringPlays) {
                if ("SO".equals(map.get("period"))) {
                    return true;
                }
            }
            return false;
        }

        public boolean isOverTime() {
            for (Map<String, Object> map : scoringPlays) {
                if ("OT".equals(map.get("period"))) {
                    return true;
                }
            }
            return false;
        }

    }

    public static class Player {

        final private List<Map<String, Object>> listGoal = new ArrayList<>();

        @Getter
        final private Map<String, Object> stat;

        public Player(Map<String, Object> stat) {
            this.stat = stat;
        }

        public int getGoals() {
            return Integer.parseInt(stat.getOrDefault("goals", "0").toString());
        }

        //🥅 Броски по воротам – shots
        public int getShots() {
            return Integer.parseInt(stat.getOrDefault("shots", "0").toString());
        }

        //🏒 Передачи – assists
        public int getAssists() {
            return Integer.parseInt(stat.getOrDefault("assists", "0").toString());
        }

        //🥷 Силовые приемы – hits
        public int getHits() {
            return Integer.parseInt(stat.getOrDefault("hits", "0").toString());
        }

        //🥊 Штрафные минуты – penaltiesInMinutes
        public int getPenaltiesInMinutes() {
            return Integer.parseInt(stat.getOrDefault("penaltiesInMinutes", "0").toString());
        }

        //⏰ Время на льду – timeOnIce
        public String getTimeOnIce() {
            return stat.getOrDefault("timeOnIce", "00:00").toString();
        }

        public String getPlayerID() {
            return stat.getOrDefault("playerID", "0").toString();
        }

        public String getLongName() {
            if (UtilNHL.isOvi(getPlayerID())) {
                return "Александр Овечкин";
            } else {
                return stat.getOrDefault("longName", "--").toString();
            }
        }

        public void addGoal(Map<String, Object> goal) {
            listGoal.add(goal);
        }

        public String getFinishTimeScore() {
            List<String> result = new ArrayList<>();
            getSortByTimeListGoal(UtilListSort.Type.ASC).forEach(map -> {
                String period = map.getOrDefault("period", "").toString();
                if (period != null
                        && !period.isEmpty()
                        && !period.equals("SO")
                ) {
                    result.add(
                            map.getOrDefault("scoreTime", "--")
                                    + ", "
                                    + periodExpandRu(period)
                    );
                }
            });
            if (!result.isEmpty()) {
                return "(" + String.join(" | ", result) + ")";
            }
            return "";
        }

        public static Player getPlayerOrEmpty(String idPlayer) {
            //TODO переделать по человечески через кеш
            Map<String, Object> about = new HashMap<>();
            try {
                Map<String, Object> map = NHLPlayerList.findById(idPlayer, NHLPlayerList.getExample());
                if (map != null) {
                    about.putAll(map);
                }
            } catch (Throwable e) {
                App.error(e);
            }
            return new Player(about);
        }

        public List<Map<String, Object>> getSortByTimeListGoal(UtilListSort.Type type) {
            return UtilListSort.sort(listGoal, type, map -> {
                try {
                    Object time = map.getOrDefault("scoreTime", "00:00");
                    return UtilDate.getTimestamp(time != null ? time.toString() : "00:00", "HH:mm");
                } catch (Throwable th) {
                    App.error(th);
                }
                return 0L;
            });
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
