package ru.jamsys.telegram.template;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.flat.template.twix.TemplateTwix;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilNHL;
import ru.jamsys.telegram.GameEventData;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class GameEventTemplateOvi {

    final GameEventData gameEventData;

    private int goalsInSeason;

    private int goalsInCareer;

    private int score;

    private int gretzkyOffset;

    private String gretzkyOffsetPostfix;

    private String gretzkyOffsetPrefix;

    private String finishDetail = "";

    Map<GameEventData.Action, String> template = new HashMapBuilder<GameEventData.Action, String>()
            .append(GameEventData.Action.START_GAME, """
                    Начало игры ${gameAbout}""")
            .append(GameEventData.Action.NOT_PLAY, """
                    ❌ Александр Овечкин не принимает участие""")
            .append(GameEventData.Action.GOAL, """
                    🚨 ГОООЛ! ${time}. Александр Овечкин забивает свой ${goalsInCareer}-й гол в карьере! До рекорда Гретцки ${gretzkyOffsetPrefix} ${gretzkyOffset} ${gretzkyOffsetPostfix}.
                    ${gameScore}""")
            .append(GameEventData.Action.CORRECTION, """
                   ✍️ Корректировка. Предыдущий гол был записан на другого игрока.
                   ${gameScore}""")
            .append(GameEventData.Action.CANCEL, """
                    ❌ Гол отменён! До рекорда Гретцки ${gretzkyOffsetPrefix} ${gretzkyOffset} ${gretzkyOffsetPostfix}.
                    ${gameScore}""")
            .append(GameEventData.Action.FINISH_GAME, """
                    Матч завершен.${finishDetail}
                    ${gameScore}.
                    
                    Статистика Александра Овечкина в матче:
                    🎯 Голы: ${scoredGoal} ${time}
                    🥅 Броски по воротам: ${scoredShots}
                    🏒 Передачи: ${scoredAssists}
                    🌟 Очки: ${score}
                    🥷 Силовые приемы: ${scoredHits}
                    🥊 Штрафные минуты: ${scoredPenaltiesInMinutes}
                    ⏰ Время на льду: ${scoredTimeOnIce}""");

    public GameEventTemplateOvi(GameEventData gameEventData) {
        this.gameEventData = gameEventData;
    }

    @Override
    public String toString() {
        goalsInSeason = gameEventData.getScoredPrevGoal() + gameEventData.getScoredGoal();
        goalsInCareer = goalsInSeason + gameEventData.getScoredLastSeason();
        gretzkyOffset = UtilNHL.getScoreGretzky() - (goalsInCareer);
        gretzkyOffsetPostfix = Util.digitTranslate(gretzkyOffset, "гол", "гола", "голов");
        gretzkyOffsetPrefix = Util.digitTranslate(gretzkyOffset, "остался", "осталось", "осталось");
        score = gameEventData.getScoredGoal() + gameEventData.getScoredAssists();

        if (gameEventData.isOverTime()) {
            finishDetail = " Победа в дополнительное время.";
        }
        if (gameEventData.isPenaltyShot()) {
            finishDetail = " Победа по буллитам.";
        }

        Map<String, String> arg = new LinkedHashMap<>();
        extend(arg, gameEventData);
        extend(arg, gameEventData.getPlayer());
        arg.put("playerName", gameEventData.getPlayer().getLongName());
        arg.put("playerNameWithTeamAbv", gameEventData.getPlayer().getLongNameWithTeamAbv());
        extend(arg, this);
        return TemplateTwix.template(template.get(gameEventData.getAction()), arg, true);
    }

    private void extend(Map<String, String> arg, Object object) {
        ObjectMapper om = new ObjectMapper();
        om.convertValue(object, new TypeReference<Map<String, Object>>() {
        }).forEach((key, value) -> {
            if (value != null) {
                if (value instanceof List<?>) {
                    @SuppressWarnings("unchecked") List<String> value1 = (List<String>) value;
                    arg.put(key, String.join(", ", value1));
                } else {
                    arg.put(key, value.toString());
                }
            }
        });
    }

}
