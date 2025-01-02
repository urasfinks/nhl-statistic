package ru.jamsys.tank.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
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
        return String.format("%s (%s)", player.getOrDefault("longName", "--"), player.getOrDefault("team", "--"));
    }

    public static String getPlayerName(Player player) {
        return String.format("%s (%s)", player.getLongName(), player.getTeam());
    }

    public static String getExample() throws IOException {
        return UtilFileResource.getAsString("example/player_id.json");
    }

    public static List<Map<String, Object>> findByName(String userName, String json) throws Throwable {
        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = UtilJson.toObject(json, Map.class);
        List<Map<String, Object>> result = new ArrayList<>();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> selector = (List<Map<String, Object>>) UtilJson.selector(parsed, "body");
        String lowerUserName = userName.toLowerCase();
        selector.forEach(map -> {
            if (map.getOrDefault("longName", "--").toString().toLowerCase().contains(lowerUserName)) {
                result.add(map);
            }
        });
        return result;
    }

    public static Map<String, Object> findById(String userId, String json) throws Throwable {
        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = UtilJson.toObject(json, Map.class);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> selector = (List<Map<String, Object>>) UtilJson.selector(parsed, "body");
        for (Map<String, Object> stringObjectMap : selector) {
            if (stringObjectMap.getOrDefault("playerID", "0").toString().equals(userId)) {
                return stringObjectMap;
            }
        }
        return null;
    }

    @Setter
    @Accessors(chain = true)
    @ToString
    public static class Player {

        String pos;

        String playerID;

        String team;

        String longName;

        String teamID;

        public String getPlayerID() {
            return requireNonNullEmptyElse(playerID, "0");
        }

        public String getTeam() {
            return requireNonNullEmptyElse(team, "--");
        }

        public String getLongName() {
            return requireNonNullEmptyElse(longName, "--");
        }

        public String getTeamID() {
            return requireNonNullEmptyElse(teamID, "0");
        }

        public static Player fromMap(Map<String, Object> map) {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.convertValue(map, Player.class);
        }

        private static String requireNonNullEmptyElse(String data, String def) {
            if (data == null || data.isEmpty()) {
                return def;
            }
            return data;
        }

    }

}
