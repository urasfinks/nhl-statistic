package ru.jamsys;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.tank.data.NHLPlayerList;

import java.util.List;
import java.util.Map;

class NHLPlayerStatListTest {

    @Test
    void findByName() throws Throwable {
        List<Map<String, Object>> list = NHLPlayerList.findByName("Caden", NHLPlayerList.getExample());
        Assertions.assertEquals("[{pos=D, playerID=5188611, team=SEA, longName=Caden Price, teamID=25}]", list.toString());
    }

    @Test
    void findById() throws Throwable {
        NHLPlayerList.Player player = NHLPlayerList.findById("5188611", NHLPlayerList.getExample());
        assert player != null;
        Assertions.assertEquals("NHLPlayerList.Player(pos=D, playerID=5188611, team=SEA, longName=Caden Price, teamID=25)", player.toString());
    }

}