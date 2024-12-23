package ru.jamsys.tank.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.jamsys.core.flat.util.UtilFileResource;
import ru.jamsys.core.flat.util.UtilJson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NHLPlayerList {

    public static String getUri() {
        return "/getNHLPlayerList";
    }

    public static String getPlayerName(Map<String, Object> player) {
        return String.format("%s (%s)", player.get("longName"), player.get("team"));
    }

    public static String getExample() throws IOException {
        return UtilFileResource.getAsString("example/player_id.json");
    }

    public static List<Map<String, Object>> findByName(String userName, String json) throws Throwable {
        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = UtilJson.toObject(json, Map.class);
        List<Map<String, Object>> result = new ArrayList<>();
        if (parsed.containsKey("error")) {
            throw new RuntimeException(parsed.get("error").toString());
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> selector = (List<Map<String, Object>>) UtilJson.selector(parsed, "body");
        String lowerUserName = userName.toLowerCase();
        selector.forEach(stringObjectMap -> {
            if (stringObjectMap.get("longName").toString().toLowerCase().contains(lowerUserName)) {
                result.add(stringObjectMap);
            }
        });
        return result;
    }

    public static Map<String, Object> findById(String userId, String json) throws Throwable {
        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = UtilJson.toObject(json, Map.class);
        if (parsed.containsKey("error")) {
            throw new RuntimeException(parsed.get("error").toString());
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> selector = (List<Map<String, Object>>) UtilJson.selector(parsed, "body");
        for (Map<String, Object> stringObjectMap : selector) {
            if (stringObjectMap.get("playerID").toString().equals(userId)) {
                return stringObjectMap;
            }
        }
        return null;
    }

    @Getter
    @Setter
    @ToString
    public static class Player {

        String pos;
        String playerID;
        String team;
        String longName;
        String teamID;

        public static Player fromMap(Map<String, Object> map) {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.convertValue(map, Player.class);
        }
    }

}
