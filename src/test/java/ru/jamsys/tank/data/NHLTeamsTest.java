package ru.jamsys.tank.data;

import org.junit.jupiter.api.Test;
import ru.jamsys.core.flat.util.UtilJson;

import java.util.Map;

class NHLTeamsTest {

    @Test
    void getTeams() throws Throwable {
        Map<String, Object> teams = NHLTeams.getTeams(NHLTeams.getExample());
        System.out.println(UtilJson.toStringPretty(teams, "{}"));
    }
}