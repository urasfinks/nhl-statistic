package ru.jamsys;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.tank.data.NHLBoxScore;

import java.util.LinkedHashMap;
import java.util.Map;

class NHLBoxScoreTest {

    @Test
    void getPlayerStat() throws Throwable {
        Map<String, Integer> playerStat = NHLBoxScore.getPlayerStat(NHLBoxScore.getExample());
        Assertions.assertEquals("{2562637=0, 4419682=0, 3041999=0, 2976848=0, 2500990=0, 4697457=0, 4565257=1, 4565235=0, 4697654=0, 3900169=1, 4781635=0, 3891952=0, 2562601=0, 3149633=0, 5474=0, 2976839=0, 3025614=0, 4197146=0, 4233668=0, 4233685=0, 2563036=0, 4697382=0, 3069397=0, 4697468=0, 4697402=0, 4271998=0, 4565223=0, 5149170=0, 4392072=0, 4697440=0, 2564164=0, 4352770=0, 4352750=0, 4894222=0, 4697406=0, 3114757=0}", playerStat.toString());
    }

    @Test
    void getDiff() throws Throwable {
        Map<String, Integer> playerStatLast = NHLBoxScore.getPlayerStat(NHLBoxScore.getExample());
        Map<String, Integer> playerStatCurrent = new LinkedHashMap<>(playerStatLast);

        playerStatCurrent.put("2562637", 1);
        Assertions.assertEquals("{2562637=1}", NHLBoxScore.getDiff(playerStatLast, playerStatCurrent).toString());

        playerStatCurrent.put("2562637", 2);
        Assertions.assertEquals("{2562637=2}", NHLBoxScore.getDiff(playerStatLast, playerStatCurrent).toString());

        playerStatCurrent.put("4565257", 1);
        Assertions.assertEquals("{2562637=2}", NHLBoxScore.getDiff(playerStatLast, playerStatCurrent).toString());

        playerStatCurrent.put("4565257", 2);
        Assertions.assertEquals("{2562637=2, 4565257=1}", NHLBoxScore.getDiff(playerStatLast, playerStatCurrent).toString());
    }

}