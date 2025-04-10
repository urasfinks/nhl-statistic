package ru.jamsys.tank.data;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class NHLTeamsTest {

    @Test
    void getTeams() {
        NHLTeams.Team wsh = NHLTeams.teams.getByAbv("WSH");
        Assertions.assertEquals("Washington Capitals (WSH)", wsh.getAbout());
        NHLTeams.Team vgk = NHLTeams.teams.getById("30");
        Assertions.assertEquals("Vegas Golden Knights (VGK)", vgk.getAbout());
        Assertions.assertEquals(32, NHLTeams.teams.getListTeam().size());
    }

    @Test
    void btButton() {
        String btn = "Hello; https://ya.ru";
        String btnTitle = btn.substring(0, btn.indexOf(";")).trim();
        String btnUrl = btn.substring(btn.indexOf(";") + 1).trim();
        Assertions.assertEquals("Hello", btnTitle);
        Assertions.assertEquals("https://ya.ru", btnUrl);
    }
}