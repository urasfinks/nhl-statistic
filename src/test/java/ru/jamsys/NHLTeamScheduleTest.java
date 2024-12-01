package ru.jamsys;

import org.junit.jupiter.api.Test;

class NHLTeamScheduleTest {

    @Test
    void findGame() throws Throwable {
        NHLTeamSchedule.findGame(NHLTeamSchedule.getExample());
    }
}