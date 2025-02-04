package ru.jamsys.telegram.template;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.flat.util.UtilFileResource;
import ru.jamsys.core.flat.util.UtilNHL;
import ru.jamsys.tank.data.NHLBoxScore;
import ru.jamsys.tank.data.NHLPlayerList;
import ru.jamsys.telegram.GameEventData;

class GameEventTemplateTest {

    @Test
    void testToString() {

        NHLPlayerList.Player player = UtilNHL.getOvi();
        GameEventData gameEventData = new GameEventData(
                GameEventData.Action.GOAL,
                "any",
                "Washington Capitals (WSH) 🆚 Detroit Red Wings (DET)",
                "Washington Capitals (WSH) 1 - 0 Detroit Red Wings (DET)",
                player,
                "14:14, 2-й период"
        );
        gameEventData
                .setAction(GameEventData.Action.GOAL)
                .setScoredGoal(2)
                .setScoredLastSeason(UtilNHL.getOviScoreLastSeason())
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
                new GameEventTemplate(gameEventData.setAction(GameEventData.Action.START_GAME)).toString()
        );

        Assertions.assertEquals("""
                        🚨 ГОООЛ! 14:14, 2-й период. Александр Овечкин (WSH) забивает свой 18-й гол в сезоне!
                        Washington Capitals (WSH) 1 - 0 Detroit Red Wings (DET)""",
                new GameEventTemplate(gameEventData.setAction(GameEventData.Action.GOAL)).toString()
        );

        Assertions.assertEquals("""
                        ❌ Гол отменён! Александр Овечкин (WSH)
                        Washington Capitals (WSH) 1 - 0 Detroit Red Wings (DET)""",
                new GameEventTemplate(gameEventData.setAction(GameEventData.Action.CANCEL)).toString()
        );

        Assertions.assertEquals("""
                        ❌ Александр Овечкин (WSH) не принимал участие""",
                new GameEventTemplate(gameEventData.setAction(GameEventData.Action.NOT_PLAY)).toString()
        );

        Assertions.assertEquals("""
                        ✍️ Корректировка. Предыдущий гол Александр Овечкин (WSH) был записан на другого игрока.
                        Washington Capitals (WSH) 1 - 0 Detroit Red Wings (DET)""",
                new GameEventTemplate(gameEventData.setAction(GameEventData.Action.CORRECTION)).toString()
        );

    }

    @Test
    void testToString2() {
        NHLPlayerList.Player player = UtilNHL.getOvi();
        GameEventData gameEventData = new GameEventData(
                GameEventData.Action.GOAL,
                "any",
                "Washington Capitals (WSH) 🆚 Detroit Red Wings (DET)",
                "Washington Capitals (WSH) 1 - 0 Detroit Red Wings (DET)",
                player,
                "(01:00, 1 период; 02:56, 2 период)"
        );
        gameEventData
                .setAction(GameEventData.Action.GOAL)
                .setScoredGoal(2)
                .setScoredLastSeason(UtilNHL.getOviScoreLastSeason())
                .setScoredPrevGoal(16)

                .setScoredAssists(1)
                .setScoredShots(2)
                .setScoredAssists(3)
                .setScoredHits(4)
                .setScoredPenaltiesInMinutes(5)
                .setScoredTimeOnIce("15:00")
        ;

        Assertions.assertEquals("""
                        Матч завершен.
                        Washington Capitals (WSH) 1 - 0 Detroit Red Wings (DET).
                        
                        Статистика Александра Овечкина в матче:
                        🎯 Голы: 2 (01:00, 1 период; 02:56, 2 период)
                        🥅 Броски по воротам: 2
                        🏒 Передачи: 3
                        🌟 Очки: 5
                        🥷 Силовые приемы: 4
                        🥊 Штрафные минуты: 5
                        ⏰ Время на льду: 15:00""",
                new GameEventTemplateOvi(gameEventData.setAction(GameEventData.Action.FINISH_GAME)).toString()
        );
    }

    @Test
    void getEvent() throws Throwable {
        NHLBoxScore.Instance currentBoxScore = new NHLBoxScore.Instance(UtilFileResource.getAsString("example/PenaltyShot.json"));
        NHLPlayerList.Player ovi = UtilNHL.getOvi();
        NHLBoxScore.PlayerStat playerStat = currentBoxScore.getPlayerStat(ovi.getPlayerID());
        GameEventData gameEventData = new GameEventData(
                GameEventData.Action.FINISH_GAME,
                currentBoxScore.getIdGame(),
                currentBoxScore.getAboutGame(),
                currentBoxScore.getScoreGame(),
                playerStat.getPlayerOrEmpty(),
                playerStat.getFinishTimeScore()
        )
                .setScoredGoal(playerStat.getGoals())
                .setScoredAssists(playerStat.getAssists())
                .setScoredShots(playerStat.getShots())
                .setScoredHits(playerStat.getHits())
                .setScoredPenaltiesInMinutes(playerStat.getPenaltiesInMinutes())
                .setScoredTimeOnIce(playerStat.getTimeOnIce())
                .setPenaltyShot(currentBoxScore.isPenaltyShot())
                .setOverTime(currentBoxScore.isOverTime());
        Assertions.assertEquals("""
              Матч завершен. Победа по буллитам.
              Washington Capitals (WSH) 3 - 3 Buffalo Sabres (BUF).
              
              Статистика Александра Овечкина в матче:
              🎯 Голы: 0\s
              🥅 Броски по воротам: 5
              🏒 Передачи: 0
              🌟 Очки: 0
              🥷 Силовые приемы: 0
              🥊 Штрафные минуты: 0
              ⏰ Время на льду: 16:43""", new GameEventTemplateOvi(gameEventData).toString());

        gameEventData.setPenaltyShot(false);
        Assertions.assertEquals("""
              Матч завершен. Победа в дополнительное время.
              Washington Capitals (WSH) 3 - 3 Buffalo Sabres (BUF).
              
              Статистика Александра Овечкина в матче:
              🎯 Голы: 0\s
              🥅 Броски по воротам: 5
              🏒 Передачи: 0
              🌟 Очки: 0
              🥷 Силовые приемы: 0
              🥊 Штрафные минуты: 0
              ⏰ Время на льду: 16:43""", new GameEventTemplateOvi(gameEventData).toString());

    }
}