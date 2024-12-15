package ru.jamsys.tank.data;

import ru.jamsys.core.flat.util.UtilFileResource;
import ru.jamsys.core.flat.util.UtilJson;

import java.io.IOException;
import java.util.Map;

public class NHLGamesForPlayer {

    public static String getExample() throws IOException {
        return UtilFileResource.getAsString("example/4565222.json");
    }

    public static String getExampleOvechkin() throws IOException {
        return UtilFileResource.getAsString("example/3101.json");
    }

    public static Map<String, Map<String, Object>> goals(String json) throws Throwable {
        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = UtilJson.toObject(json, Map.class);
        if (parsed.containsKey("error")) {
            throw new RuntimeException(parsed.get("error").toString());
        }
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> selector = (Map<String, Map<String, Object>>) UtilJson.selector(parsed, "body");
        return selector;
    }

}
