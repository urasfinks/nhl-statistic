package ru.jamsys.tank.data;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class NHLTeamsTest {

    @Test
    void getTeams() throws Throwable {
        NHLTeams.Team wsh = NHLTeams.teams.getByAbv("WSH");
        Assertions.assertEquals("Washington Capitals (WSH)", wsh.getAbout());
        NHLTeams.Team vgk = NHLTeams.teams.getById("30");
        Assertions.assertEquals("Vegas Golden Knights (VGK)", vgk.getAbout());
    }
}