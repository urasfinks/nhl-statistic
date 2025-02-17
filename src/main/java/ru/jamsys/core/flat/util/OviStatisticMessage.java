package ru.jamsys.core.flat.util;

import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.flat.template.twix.TemplateTwix;
import ru.jamsys.core.handler.promise.PlayerStatistic;
import ru.jamsys.tank.data.NHLTeamSchedule;

import java.util.Map;

public class OviStatisticMessage {

    public static String get(PlayerStatistic playerStatistic) {
        Map<String, Object> scoreTotal = playerStatistic.getScoreTotal();
        int seasonGoals = Integer.parseInt(scoreTotal.getOrDefault("goals", "0").toString());
        int countGame = Integer.parseInt(scoreTotal.getOrDefault("countGame", "").toString());

        int assists = Integer.parseInt(scoreTotal.getOrDefault("assists", "").toString());

        int totalGoals = playerStatistic.getScoreLastSeason() + seasonGoals;
        int gretzkyOffset = UtilNHL.getScoreGretzky() - totalGoals;

        String templateNextGame = "";
        if (playerStatistic.getNextGame() != null) {
            NHLTeamSchedule.Game game = new NHLTeamSchedule.Game(playerStatistic.getNextGame());
            templateNextGame = String.format(
                    "Следующая игра: 🆚 %s, %s",
                    game.toggleTeam(UtilNHL.getOvi().getTeam()),
                    game.getMoscowDate()
            );
        }

        return TemplateTwix.template("""
                Статистика Александра Овечкина на ${currentDate}:
                🎯 Забито голов: ${totalGoals}
                🏆 До рекорда Гретцки: ${gretzkyOffset} ${gretzkyOffsetPostfix}
                📅 Сезон ${seasonTitle}: ${countGame} ${countGamePostfix}, ${seasonGoals} ${seasonGoalsPostfix}, ${assists} ${assistsPostfix}, ${score} ${scorePostfix}, ${countTailGamePrefix} ${countTailGame} ${countTailGamePostfix} в регулярном чемпионате
                📈 Темп: В среднем ${avgGoalsInGame} гола за игру в этом сезоне
                
                ${templateNextGame}
                
                📍 Время указано по МСК
                """, new HashMapBuilder<String, String>()
                .append("currentDate", playerStatistic.getDate())

                .append("totalGoals", String.valueOf(totalGoals))
                .append("totalGoalsPostfix", Util.digitTranslate(totalGoals, "гол", "гола", "голов"))

                .append("seasonGoals", String.valueOf(seasonGoals))
                .append("seasonGoalsPostfix", Util.digitTranslate(seasonGoals, "гол", "гола", "голов"))

                .append("gretzkyOffset", String.valueOf(gretzkyOffset))
                .append("gretzkyOffsetPostfix", Util.digitTranslate(gretzkyOffset, "гол", "гола", "голов"))

                .append("countTailGamePrefix", Util.digitTranslate(playerStatistic.getCountTailGame(), "остался", "осталось", "осталось"))
                .append("countTailGame", String.valueOf(playerStatistic.getCountTailGame()))
                .append("countTailGamePostfix", Util.digitTranslate(playerStatistic.getCountTailGame(), "матч", "матча", "матчей"))

                .append("avgGoalsInGame", playerStatistic.getAvgGoalsInGame().toString())

                .append("countGame", String.valueOf(countGame))
                .append("countGamePostfix", Util.digitTranslate(countGame, "матч", "матча", "матчей"))

                .append("seasonTitle", UtilNHL.seasonFormat(UtilNHL.getActiveSeasonOrNext()))
                .append("assists", String.valueOf(assists))
                .append("assistsPostfix", Util.digitTranslate(assists, "передача", "передачи", "передач"))

                .append("score", String.valueOf(assists + seasonGoals))
                .append("scorePostfix", Util.digitTranslate(assists + seasonGoals, "очко", "очка", "очков"))

                .append("templateNextGame", templateNextGame)
        ).trim();
    }
}
