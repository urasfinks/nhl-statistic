package ru.jamsys.tank.data;

import ru.jamsys.core.flat.util.UtilFileResource;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.flat.util.tank.UtilTank01;
import ru.jamsys.core.promise.Promise;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NHLPlayerList {

    public static void promiseExtensionGetPlayerList(Promise promiseSource) {
        UtilTank01.cacheRequest(promiseSource, _ -> "/getNHLPlayerList");
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
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> selector = (List<Map<String, Object>>) UtilJson.selector(parsed, "body");
        for (Map<String, Object> stringObjectMap : selector) {
            if (stringObjectMap.get("playerID").toString().equals(userId)) {
                return stringObjectMap;
            }
        }
        return null;
    }

}
