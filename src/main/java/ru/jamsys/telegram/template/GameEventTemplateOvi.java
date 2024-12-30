package ru.jamsys.telegram.template;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
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


    final GameEventData data;

    private String extra = "";

    private String action = "";

    private String scoredTitle = "";

    private int goalsInSeason;

    private int goalsInCareer;

    private int gretzkyOffset;

    public String templateGoal = """
            ${action} Александр Овечкин в этом матче забил ${scoredGoal} ${scoredTitle}${extra}. В сезоне это ${goalsInSeason}-й, в карьере ${goalsInCareer}-й гол! До рекорда Гретцки осталось ${gretzkyOffset} гола.
            ${teamsScore}""";

    public String templateCancel = """
            ${action} До рекорда Гретцки осталось ${gretzkyOffset} гола.
            ${teamsScore}""";

    public String templateCorrection = """
            ${action} ${scoredGoal} ${scoredTitle}${extra}.
            ${teamsScore}""";
    public String templateCancelCorrection = """
            ${action} ${scoredGoal} ${scoredTitle}${extra}. До рекорда Гретцки осталось ${gretzkyOffset} гола.
            ${teamsScore}""";

    public GameEventTemplateOvi(GameEventData data) {
        this.data = data;
    }

    @Override
    public String toString() {
        if (!data.getScoredEnum().isEmpty()) {
            extra = ": " + String.join(", ", data.getScoredEnum())
                    .replaceAll("undefined period", "неизвестный период")
                    .replaceAll("1st period", "1-й период")
                    .replaceAll("2nd period", "2-й период")
                    .replaceAll("3rd period", "3-й период")
                    .replaceAll("overtime", "доп. время");
        }
        goalsInSeason = data.getScoredPrevGoal() + data.getScoredGoal();
        goalsInCareer = goalsInSeason + data.getScoredLastSeason();
        gretzkyOffset = UtilNHL.getScoreGretzky() - (goalsInCareer);
        scoredTitle = Util.digitTranslate(data.getScoredGoal(), "гол", "гола", "голов");
        action = switch (data.getAction()) {
            case GameEventData.Action.GOAL -> "🚨 ГОООЛ!";
            case GameEventData.Action.CANCEL -> "❌ Гол отменён!";
            case GameEventData.Action.CANCEL_CORRECTION -> "❌✅ Корректировка.";
            case GameEventData.Action.CORRECTION -> "✅ Корректировка.";
            default -> data.getAction().toString();
        };

        Map<String, String> arg = new LinkedHashMap<>();
        extend(arg, data);
        extend(arg, this);

        if (data.getAction().equals(GameEventData.Action.START_GAME)) {
            return "Начало игры Washington Capitals (WSH) 🆚 " + data.getGameName();
        } else if (data.getAction().equals(GameEventData.Action.FINISH_GAME)) {

        } else if (data.getAction().equals(GameEventData.Action.GOAL)) {
            return TemplateTwix.template(templateGoal, arg, true);
        } else if (data.getAction().equals(GameEventData.Action.CANCEL)) {
            return TemplateTwix.template(templateCancel, arg, true);
        } else if (data.getAction().equals(GameEventData.Action.CORRECTION)) {
            return TemplateTwix.template(templateCorrection, arg, true);
        } else if (data.getAction().equals(GameEventData.Action.CANCEL_CORRECTION)) {
            return TemplateTwix.template(templateCancelCorrection, arg, true);
        }
        return null;
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
