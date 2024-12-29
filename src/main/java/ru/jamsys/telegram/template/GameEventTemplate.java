package ru.jamsys.telegram.template;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.flat.template.twix.TemplateTwix;
import ru.jamsys.core.flat.util.UtilNHL;
import ru.jamsys.telegram.GameEventData;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class GameEventTemplate {


    final GameEventData data;

    private String extra = "";

    private int goalsInSeason;

    private int goalsInCareer;

    private int gretzkyOffset;

    public String template = "${action}! Game ${gameName}. ${playerName} scored ${scoredGoal} ${scoredTitle}${extra}. He has ${goalsInSeason} goals in season, ${goalsInCareer} goals in career and only ${gretzkyOffset} goals till Gretzky all-time record";

    public GameEventTemplate(GameEventData data) {
        this.data = data;
    }

    @Override
    public String toString() {
        if (!data.getScoredEnum().isEmpty()) {
            extra = ": " + String.join(", ", data.getScoredEnum());
        }

        goalsInSeason = data.getScoredBeforeCurrentSeason() + data.getScoredGoal();
        goalsInCareer = goalsInSeason + data.getScoredBeforeCurrentSeason();
        gretzkyOffset = UtilNHL.getScoreGretzky() - (goalsInCareer);

        Map<String, String> arg = new LinkedHashMap<>();
        extend(arg, this);
        extend(arg, data);
        return TemplateTwix.template(template, arg, true);
    }

    private void extend(Map<String, String> arg, Object object) {
        ObjectMapper om = new ObjectMapper();
        om.convertValue(object, new TypeReference<Map<String, Object>>() {
        }).forEach((key, value) -> {
            if (value != null) {
                if (value instanceof List<?>) {
                    @SuppressWarnings("unchecked")
                    List<String> value1 = (List<String>) value;
                    arg.put(key, String.join(", ", value1));
                } else {
                    arg.put(key, value.toString());
                }
            }
        });
    }

}
