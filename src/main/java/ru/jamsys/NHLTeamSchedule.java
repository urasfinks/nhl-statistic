package ru.jamsys;

import ru.jamsys.core.flat.util.UtilFileResource;
import ru.jamsys.core.flat.util.UtilJson;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class NHLTeamSchedule {

    public static String getExample() throws IOException {
        return UtilFileResource.getAsString("example/getNHLTeamSchedule.json");
    }

    public static void findGame(String json) throws Throwable {
        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = UtilJson.toObject(json, Map.class);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> selector = (List<Map<String, Object>>) UtilJson.selector(parsed, "body.schedule");
        selector.forEach(stringObjectMap -> {
            if (stringObjectMap.containsKey("gameStatus") && stringObjectMap.get("gameStatus").equals("Scheduled")) {
                System.out.println(stringObjectMap);
            }
        });
    }

}
