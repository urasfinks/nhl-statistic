package ru.jamsys.tank.data;

import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilFileResource;
import ru.jamsys.core.flat.util.UtilJson;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class NHLGamesForPlayer {

    public static String getUri(String idPlayer) {
        return "/getNHLGamesForPlayer?playerID=" + idPlayer + "&numberOfGames=10000";
    }

    public static String getExampleOvechkin() throws IOException {
        return UtilFileResource.getAsString("example/3101.json");
    }

    public static Map<String, Integer> getOnlyGoals(String json) throws Throwable {
        return getOnlyGoalsFilter(json, null);
    }

    public static Map<String, Integer> getOnlyGoalsFilter(String json, List<String> idGames) throws Throwable {
        Map<String, Integer> result = new LinkedHashMap<>();
        parseBody(json).forEach(
                (idGame, map) -> {
                    if (idGames == null || idGames.contains(idGame)) {
                        result.put(
                                idGame,
                                Integer.parseInt(map.get("goals").toString())
                        );
                    }
                }
        );
        return result;
    }

    public static Map<String, Map<String, Object>> parseBody(String json) throws Throwable {
        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = UtilJson.toObject(json, Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> selector = (Map<String, Map<String, Object>>) UtilJson.selector(parsed, "body");
        return selector;
    }

    public static Map<String, Object> getAggregateStatistic(List<Map<String, Object>> data) {
        Map<String, AtomicInteger> pre = new HashMap<>();
        Map<String, Object> result = new HashMap<>();
        AtomicInteger countGame = new AtomicInteger(0);
        data.forEach((map) -> {
            if (map.containsKey("countGame")) {
                countGame.addAndGet(Integer.parseInt(map.get("countGame").toString()));
            } else {
                countGame.incrementAndGet();
            }
            map.forEach((key, value) -> {
                String prepValue = value.toString();
                if (
                        key.equals("timeOnIce")
                                || key.equals("shortHandedTimeOnIce")
                                || key.equals("powerPlayTimeOnIce")
                ) {
                    prepValue = String.valueOf(getSec(prepValue));
                }

                if (Util.isNumeric(prepValue)) {
                    pre.computeIfAbsent(key, _ -> new AtomicInteger(0))
                            .addAndGet(Integer.parseInt(prepValue));
                }
            });
        });
        pre.remove("playerID");
        pre.remove("teamID");
        pre.put("countGame", countGame);
        pre.forEach((key, atomicInteger) -> result.put(key, atomicInteger.get()));
        result.put("timeOnIce", getSecFormat((int) result.getOrDefault("timeOnIce", 0)));
        result.put("shortHandedTimeOnIce", getSecFormat((int) result.getOrDefault("shortHandedTimeOnIce", 0)));
        result.put("powerPlayTimeOnIce", getSecFormat((int) result.getOrDefault("powerPlayTimeOnIce", 0)));
        return result;
    }

    public static Map<String, Object> getAggregateStatistic(String json, List<String> idGames) throws Throwable {
        List<Map<String, Object>> data = new ArrayList<>();
        parseBody(json).forEach((idGame, map) -> {
            if (idGames != null && !idGames.contains(idGame)) {
                return;
            }
            data.add(map);
        });
        return getAggregateStatistic(data);
    }

    public static int getSec(String time) {
        if (time == null || time.isEmpty()) {
            return 0;
        }
        String min = time.substring(0, time.indexOf(":"));
        String sec = time.substring(time.indexOf(":") + 1);
        return Integer.parseInt(min) * 60 + Integer.parseInt(sec);
    }

    public static String getSecFormat(int totalSeconds) {
        int min = totalSeconds / 60;
        int sec = totalSeconds % 60;
        return String.format("%02d:%02d", min, sec);
    }

}
