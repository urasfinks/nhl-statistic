package ru.jamsys.core.flat.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.tank.data.NHLPlayerList;

import java.text.ParseException;

import static org.junit.jupiter.api.Assertions.assertTrue;

class UtilNHLTest {

    @Test
    void getScoreGretzky() {
        Assertions.assertEquals(894, UtilNHL.getScoreGretzky());
    }

    @Test
    void getActiveSeasonOrNext() throws ParseException {
        Assertions.assertEquals(2025, UtilNHL.getActiveSeasonOrNext("30.12.2024", "dd.MM.yyyy"));
        Assertions.assertEquals(2025, UtilNHL.getActiveSeasonOrNext("01.01.2025", "dd.MM.yyyy"));

        Assertions.assertEquals(2025, UtilNHL.getActiveSeasonOrNext("2024-12-20", "yyyy-MM-dd"));
        Assertions.assertEquals(2025, UtilNHL.getActiveSeasonOrNext("2024-11-20", "yyyy-MM-dd"));
        Assertions.assertEquals(2025, UtilNHL.getActiveSeasonOrNext("2024-10-20", "yyyy-MM-dd"));
        Assertions.assertEquals(2025, UtilNHL.getActiveSeasonOrNext("2024-10-01", "yyyy-MM-dd"));

        Assertions.assertNull(UtilNHL.getActiveSeasonOrNext("2024-09-30", "yyyy-MM-dd"));
        Assertions.assertNull(UtilNHL.getActiveSeasonOrNext("2024-05-01", "yyyy-MM-dd"));

        Assertions.assertEquals(2024, UtilNHL.getActiveSeasonOrNext("2024-04-30", "yyyy-MM-dd"));
        Assertions.assertEquals(2024, UtilNHL.getActiveSeasonOrNext("2024-01-01", "yyyy-MM-dd"));

        Assertions.assertNull(UtilNHL.getActiveSeasonOrNext("2023-05-01", "yyyy-MM-dd"));

        Assertions.assertEquals(2023, UtilNHL.getActiveSeasonOrNext("2023-04-30", "yyyy-MM-dd"));
    }

    @Test
    void seasonFormat() {
        Assertions.assertEquals("Сезон не определён", UtilNHL.seasonFormat(null));
        Assertions.assertEquals("Сезон не определён", UtilNHL.seasonFormat(1));
        Assertions.assertEquals("Сезон не определён", UtilNHL.seasonFormat(11));
        Assertions.assertEquals("Сезон не определён", UtilNHL.seasonFormat(111));
        Assertions.assertEquals("1110/11", UtilNHL.seasonFormat(1111));
        Assertions.assertEquals("2024/25", UtilNHL.seasonFormat(2025));
        Assertions.assertEquals("2023/24", UtilNHL.seasonFormat(2024));
    }

    @Test
    void getOvi() {
        NHLPlayerList.Player ovi = UtilNHL.getOvi();
        Assertions.assertEquals("3101", ovi.getPlayerID());
        Assertions.assertEquals("WSH", ovi.getTeam());
        Assertions.assertEquals("31", ovi.getTeamID());
    }

    @Test
    void getOviScoreLastSeason() {
        Assertions.assertEquals(853, UtilNHL.getOviScoreLastSeason());
    }

    @Test
    void isOvi() {
        NHLPlayerList.Player ovi = UtilNHL.getOvi();
        assertTrue(UtilNHL.isOvi(ovi.getPlayerID()));
    }

    @Test
    void isOviGame() {
        Assertions.assertFalse(UtilNHL.isOviGame(null));
        assertTrue(UtilNHL.isOviGame("20241228_WSH@TOR"));
        assertTrue(UtilNHL.isOviGame("20241113_TOR@WSH"));
        Assertions.assertFalse(UtilNHL.isOviGame("20241129_NYR@PHI"));
        Assertions.assertFalse(UtilNHL.isOviGame("20241129_NYRWSH@PHI"));
        Assertions.assertFalse(UtilNHL.isOviGame("20241129_WSHNYR@PHI"));
        Assertions.assertFalse(UtilNHL.isOviGame("20241129_NYR@PHIWSH"));
        Assertions.assertFalse(UtilNHL.isOviGame("20241129_NYR@WSHPHI"));
    }
}