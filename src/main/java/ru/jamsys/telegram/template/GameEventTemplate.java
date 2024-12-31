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
public class GameEventTemplate {

    final GameEventData data;

    private int goalsInSeason;

    private int goalsInCareer;

    private int score;

    private int gretzkyOffset;

    private String gretzkyOffsetPostfix;

    Map<GameEventData.Action, String> template = new HashMapBuilder<GameEventData.Action, String>()
            .append(GameEventData.Action.START_GAME, """
                    Начало игры ${gameName}""")
            .append(GameEventData.Action.GOAL, """
                    🚨 ГОООЛ! ${time}. ${playerName} забивает свой ${goalsInSeason}-й гол в сезоне!.
                    ${teamsScore}""")
            .append(GameEventData.Action.CANCEL, """
                    ❌ Гол отменён!.
                    ${teamsScore}""")
            .append(GameEventData.Action.FINISH_GAME, """
                    Матч завершен.
                    ${teamsScore}.
                    
                    Статистика ${playerName} в матче:
                    🎯 Голы: ${scoredGoal}
                    🥅 Броски по воротам: ${scoredShots}
                    🏒 Передачи: ${scoredAssists}
                    🌟 Очки: ${score}
                    🥷 Силовые приемы: ${scoredHits}
                    🥊 Штрафные минуты: ${scoredPenaltiesInMinutes}
                    ⏰ Время на льду: ${scoredTimeOnIce}""");

    public GameEventTemplate(GameEventData data) {
        this.data = data;
    }

    @Override
    public String toString() {
        goalsInSeason = data.getScoredPrevGoal() + data.getScoredGoal();
        goalsInCareer = goalsInSeason + data.getScoredLastSeason();
        gretzkyOffset = UtilNHL.getScoreGretzky() - (goalsInCareer);
        gretzkyOffsetPostfix = Util.digitTranslate(gretzkyOffset, "гол", "гола", "голов");
        score = data.getScoredGoal() + data.getScoredAssists();

        Map<String, String> arg = new LinkedHashMap<>();
        extend(arg, data);
        extend(arg, this);
        return TemplateTwix.template(template.get(data.getAction()), arg, true);
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
