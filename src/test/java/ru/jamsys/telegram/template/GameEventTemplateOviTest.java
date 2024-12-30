package ru.jamsys.telegram.template;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.extension.builder.ArrayListBuilder;
import ru.jamsys.core.flat.util.UtilNHL;
import ru.jamsys.tank.data.NHLPlayerList;
import ru.jamsys.telegram.GameEventData;

class GameEventTemplateOviTest {

    @Test
    void testToString() {

        NHLPlayerList.Player player = UtilNHL.getOvi();
        GameEventData gameEventData = new GameEventData();
        gameEventData
                .setAction(GameEventData.Action.GOAL)
                .setScoredGoal(2)
                .setScoredEnum(new ArrayListBuilder<String>()
                        .append("10:19 3nd period")
                        .append("10:18 2nd period")
                )
                .setPlayerName(NHLPlayerList.getPlayerName(player))
                .setGameName("Detroit Red Wings (DET)")
                .setScoredLastSeason(UtilNHL.getOviScoreLastSeason())
                .setTeamsScore("Washington Capitals (WSH) 1 - 0 Detroit Red Wings (DET)")
                .setScoredPrevGoal(16);

        Assertions.assertEquals(
                "Начало игры Washington Capitals (WSH) 🆚 Detroit Red Wings (DET)",
                new GameEventTemplateOvi(gameEventData.setAction(GameEventData.Action.START_GAME)).toString()
        );

        Assertions.assertEquals(
                """
                        🚨 ГОООЛ! Александр Овечкин в этом матче забил 2 гола: 10:19 3nd period, 10:18 2-й период. В сезоне это 18-й, в карьере 871-й гол! До рекорда Гретцки осталось 23 гола.
                        Washington Capitals (WSH) 1 - 0 Detroit Red Wings (DET)""",
                new GameEventTemplateOvi(gameEventData.setAction(GameEventData.Action.GOAL)).toString()
        );

        Assertions.assertEquals(
                """
                        ❌ Гол отменён! До рекорда Гретцки осталось 23 гола.
                        Washington Capitals (WSH) 1 - 0 Detroit Red Wings (DET)""",
                new GameEventTemplateOvi(gameEventData.setAction(GameEventData.Action.CANCEL)).toString()
        );

        Assertions.assertEquals(
                """
                        ✅ Корректировка. 2 гола: 10:19 3nd period, 10:18 2-й период.
                        Washington Capitals (WSH) 1 - 0 Detroit Red Wings (DET)""",
                new GameEventTemplateOvi(gameEventData.setAction(GameEventData.Action.CORRECTION)).toString()
        );

        Assertions.assertEquals(
                """
                        ❌✅ Корректировка. 2 гола: 10:18 2-й период. До рекорда Гретцки осталось 23 гола.
                        Washington Capitals (WSH) 1 - 0 Detroit Red Wings (DET)""",
                new GameEventTemplateOvi(gameEventData.setAction(GameEventData.Action.CANCEL_CORRECTION)).toString()
        );
    }
}