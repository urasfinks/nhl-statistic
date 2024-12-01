package ru.jamsys;

import org.junit.jupiter.api.Test;
import ru.jamsys.tank.data.NHLTeamSchedule;

class NHLTeamScheduleTest {

    @Test
    void findGame() throws Throwable {
        NHLTeamSchedule.findGame(NHLTeamSchedule.getExample());
    }
}