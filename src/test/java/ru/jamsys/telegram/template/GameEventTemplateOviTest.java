package ru.jamsys.telegram.template;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.flat.util.UtilNHL;
import ru.jamsys.tank.data.NHLPlayerList;
import ru.jamsys.telegram.GameEventData;

class GameEventTemplateOviTest {

    @Test
    void testToString() {

        NHLPlayerList.Player player = UtilNHL.getOvi();
        GameEventData gameEventData = new GameEventData();
        gameEventData
                .setTime("14:14, 2-й период")
                .setAction(GameEventData.Action.GOAL)
                .setScoredGoal(2)
                .setPlayerName(NHLPlayerList.getPlayerName(player))
                .setGameName("Washington Capitals (WSH) 🆚 Detroit Red Wings (DET)")
                .setScoredLastSeason(UtilNHL.getOviScoreLastSeason())
                .setTeamsScore("Washington Capitals (WSH) 1 - 0 Detroit Red Wings (DET)")
                .setScoredPrevGoal(16)

                .setScoredAssists(1)
                .setScoredShots(2)
                .setScoredAssists(3)
                .setScoredHits(4)
                .setScoredPenaltiesInMinutes(5)
                .setScoredTimeOnIce("15:00")
        ;

        Assertions.assertEquals(
                "Начало игры Washington Capitals (WSH) 🆚 Detroit Red Wings (DET)",
                new GameEventTemplateOvi(gameEventData.setAction(GameEventData.Action.START_GAME)).toString()
        );

        Assertions.assertEquals("""
                        🚨 ГОООЛ! 14:14, 2-й период. Александр Овечкин забивает свой 871-й гол в карьере! До рекорда Гретцки осталось 23 гола.
                        Washington Capitals (WSH) 1 - 0 Detroit Red Wings (DET)""",
                new GameEventTemplateOvi(gameEventData.setAction(GameEventData.Action.GOAL)).toString()
        );

        Assertions.assertEquals("""
                        ❌ Гол отменён! До рекорда Гретцки осталось 23 гола.
                        Washington Capitals (WSH) 1 - 0 Detroit Red Wings (DET)""",
                new GameEventTemplateOvi(gameEventData.setAction(GameEventData.Action.CANCEL)).toString()
        );

        Assertions.assertEquals("""
                        Матч завершен.
                        Washington Capitals (WSH) 1 - 0 Detroit Red Wings (DET).
                        
                        Статистика Александра Овечкина в матче:
                        🎯 Голы: 2
                        🥅 Броски по воротам: 2
                        🏒 Передачи: 3
                        🌟 Очки: 5
                        🥷 Силовые приемы: 4
                        🥊 Штрафные минуты: 5
                        ⏰ Время на льду: 15:00""",
                new GameEventTemplateOvi(gameEventData.setAction(GameEventData.Action.FINISH_GAME)).toString()
        );

    }
}