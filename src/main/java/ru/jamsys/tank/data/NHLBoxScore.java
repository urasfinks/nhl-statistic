package ru.jamsys.tank.data;

import ru.jamsys.core.App;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.flat.util.*;
import ru.jamsys.telegram.NotificationDataAndTemplate;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class NHLBoxScore {

    public static String getUri(String idGame) {
        try {
            return "/getNHLBoxScore?gameID=" + Util.urlEncode(idGame);
        } catch (Exception e) {
            throw new ForwardException(e);
        }
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

    public static String getExample6ChangeTime() throws IOException {
        return UtilFileResource.getAsString("example/getNHLBoxScore6_change_time.json");
    }

    public static String getExample7() throws IOException {
        return UtilFileResource.getAsString("example/getNHLBoxScore7.json");
    }

    public static String getExample7ManyChange() throws IOException {
        return UtilFileResource.getAsString("example/getNHLBoxScore7_many_change.json");
    }

    public static String getExampleError() throws IOException {
        return UtilFileResource.getAsString("example/getNHLBoxScore_error.json");
    }

    public static Map<String, Object> parseBody(String json) throws Throwable {
        if (json == null || json.isEmpty()) { //Так как в БД может быть ничего
            throw new RuntimeException("empty json");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = UtilJson.toObject(json, Map.class);
        if (parsed.containsKey("error")) {
            throw new RuntimeException(parsed.get("error").toString());
        }
        if (!parsed.containsKey("body")) {
            throw new RuntimeException("body does not exist");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) parsed.get("body");
        return body;
    }

    public static List<Map<String, Object>> getScoringPlays(String json) throws Throwable {
        if (json == null || json.isEmpty()) { //Так как в БД может быть ничего
            return new ArrayList<>();
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = UtilJson.toObject(json, Map.class);
        if (parsed.containsKey("error")) {
            throw new RuntimeException(parsed.get("error").toString());
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> selector = (List<Map<String, Object>>) UtilJson.selector(parsed, "body.scoringPlays");
        return selector;
    }

    public static Map<String, List<Map<String, Object>>> getScoringPlaysMap(String json) throws Throwable {
        Map<String, List<Map<String, Object>>> result = new HashMap<>();
        if (json == null || json.isEmpty()) { //Так как в БД может быть ничего
            return result;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = UtilJson.toObject(json, Map.class);
        if (parsed.containsKey("error")) {
            throw new RuntimeException(parsed.get("error").toString());
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> selector = (List<Map<String, Object>>) UtilJson.selector(parsed, "body.scoringPlays");

        selector.forEach(map -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> o = (Map<String, Object>) map.get("goal");
            result.computeIfAbsent(o.get("playerID").toString(), _ -> new ArrayList<>()).add(
                    new HashMapBuilder<String, Object>()
                            .append("period", map.get("period"))
                            .append("scoreTime", map.get("scoreTime"))
            );
        });
        return result;
    }

    public static String hashObject(Map<String, Object> stringObjectMap) {
        try {
            return Util.getHash(stringObjectMap.toString(), "md5");
        } catch (Throwable e) {
            App.error(e);
        }
        return null;
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

    public static List<Map<String, Object>> getNewEventScoring(String jsonLast, String jsonCurrent) throws Throwable {
        List<Map<String, Object>> scoringPlaysLast = getScoringPlays(jsonLast);
        List<Map<String, Object>> scoringPlaysCurrent = getScoringPlays(jsonCurrent);

        List<String> listLast = scoringPlaysLast.stream().map(NHLBoxScore::hashObject).toList();
        List<String> listCurrent = new ArrayList<>(scoringPlaysCurrent.stream().map(NHLBoxScore::hashObject).toList());

        for (int i = listLast.size() - 1; i >= 0; i--) {
            if (!listCurrent.isEmpty() && listCurrent.getLast().equals(listLast.get(i))) {
                listCurrent.removeLast();
                scoringPlaysCurrent.removeLast();
            } else {
                break;
            }
        }
        return scoringPlaysCurrent;
    }

    public static List<String> getEnumGame(List<Map<String, Object>> listPlaysCurrent) {
        List<String> timeGoal = new ArrayList<>();
        listPlaysCurrent.forEach(map -> timeGoal.add(map.get("scoreTime") + " " + periodExpand(map.get("period").toString())));
        return timeGoal;
    }

    public static String periodExpand(String period) {
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


    public static Map<String, NotificationDataAndTemplate> getNewEventScoringByPlayer(String last, String current) throws Throwable {

        Map<String, NotificationDataAndTemplate> result = new HashMap<>();

        Map<String, List<Map<String, Object>>> scoringPlaysLast = getScoringPlaysMap(last);
        Map<String, List<Map<String, Object>>> scoringPlaysCurrent = getScoringPlaysMap(current);

        Set<String> idPlayers = new HashSet<>();
        idPlayers.addAll(scoringPlaysLast.keySet());
        idPlayers.addAll(scoringPlaysCurrent.keySet());
        idPlayers.forEach(idPlayer -> {
            List<Map<String, Object>> listPlaysCurrent = scoringPlaysCurrent.getOrDefault(idPlayer, new ArrayList<>());
            List<Map<String, Object>> newEventScoringByPlayer = getNewEventScoringByPlayer(
                    scoringPlaysLast.getOrDefault(idPlayer, new ArrayList<>()),
                    listPlaysCurrent
            );
            if (!newEventScoringByPlayer.isEmpty()) {
                Set<String> notify = new HashSet<>();
                for (Map<String, Object> event : newEventScoringByPlayer) {
                    notify.add(event.get("type").toString());
                }
                NotificationDataAndTemplate notificationDataAndTemplate = new NotificationDataAndTemplate();

                if (notify.contains("goal")) {
                    notificationDataAndTemplate.setAction(newEventScoringByPlayer.size() > 1 ? "GOALS" : "GOAL");
                } else if (notify.size() == 1 && notify.contains("cancel")) {
                    notificationDataAndTemplate.setAction("CANCEL");
                } else if (notify.size() == 1 && notify.contains("changeScoreTime")) {
                    notificationDataAndTemplate.setAction("CORRECTION");
                } else {
                    notificationDataAndTemplate.setAction("CANCEL+CORRECTION");
                }
                notificationDataAndTemplate.setScoredTitle(listPlaysCurrent.size() > 1 ? "goals" : "goal");
                notificationDataAndTemplate.setScoredGoal(listPlaysCurrent.size());
                notificationDataAndTemplate.setScoredEnum(getEnumGame(listPlaysCurrent));
                result.put(idPlayer, notificationDataAndTemplate);
            }
        });
        return result;
    }

    public static List<Map<String, Object>> getNewEventScoringByPlayer(List<Map<String, Object>> last, List<Map<String, Object>> current) {
        List<Map<String, Object>> res = new ArrayList<>();

        last.reversed().forEach(map -> {
            for (int i = current.size() - 1; i >= 0; i--) {
                if (current.get(i).containsKey("findInLast")) {
                    continue;
                }
                if (map.get("scoreTime").equals(current.get(i).get("scoreTime"))) {
                    current.get(i).put("findInLast", true);
                    map.put("findInCurrent", true);
                    break;
                }
            }
        });
        current.reversed().forEach(map -> {
            if (!map.containsKey("findInLast")) {
                for (int i = last.size() - 1; i >= 0; i--) {
                    if (last.get(i).containsKey("findInCurrent")) {
                        continue;
                    }
                    if (map.get("scoreTime").equals(last.get(i).get("scoreTime"))) {
                        last.get(i).put("findInCurrent", true);
                        map.put("findInLast", true);
                        break;
                    }
                }
            }
        });
        List<Map<String, Object>> cancel = new ArrayList<>();
        List<Map<String, Object>> goal = new ArrayList<>();

        last.forEach(map -> {
            if (!map.containsKey("findInCurrent")) {
                map.put("type", "cancel");
                res.add(map);
                cancel.add(map);
            }
        });
        current.forEach(map -> {
            if (!map.containsKey("findInLast")) {
                map.put("type", "goal");
                res.add(map);
                goal.add(map);
            }
        });

        cancel.forEach(cancelMap -> {
            List<Map<String, Object>> reduce = new ArrayList<>();
            goal.forEach(goalMap -> {
                if (!goalMap.get("type").equals("reduceCancel")) {
                    try {
                        long secOffset = Math.abs(UtilDate.diffSecond(
                                cancelMap.get("scoreTime").toString(),
                                goalMap.get("scoreTime").toString(),
                                "H:mm"
                        ));
                        if (secOffset <= 3 * 60) {
                            reduce.add(goalMap);
                        }
                    } catch (ParseException _) {
                    }
                }
            });

            if (!reduce.isEmpty()) {
                Map<String, Object> first = UtilListSort.sort(reduce, UtilListSort.Type.ASC, map -> {
                    DateFormat dateFormat = new SimpleDateFormat("H:mm");
                    try {
                        return dateFormat.parse(map.get("scoreTime").toString()).getTime();
                    } catch (ParseException _) {
                    }
                    return 0L;
                }).getFirst();
                first.put("type", "reduceCancel");
                cancelMap.put("type", "changeScoreTime");
                cancelMap.put("newScoreTime", first.get("scoreTime"));
            }
        });
        return res.stream().filter(map -> !map.get("type").equals("reduceCancel")).toList();
    }

    public static Map<String, Object> getPlayerStat(String json, String idPlayer) {
        @SuppressWarnings("unchecked")
        Map<String, Object> res = (Map<String, Object>) UtilJson.selector(json, "$.body.playerStats." + idPlayer);
        return res;
    }

}
