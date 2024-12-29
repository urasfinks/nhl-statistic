package ru.jamsys.telegram.template;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.flat.template.twix.TemplateTwix;
import ru.jamsys.telegram.EventData;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class EventTemplate {

    public static int scoreGretzky = 894; // Кол-во голов у Gretzky

    final EventData data;

    private String extra = "";

    private int goalsInSeason;

    private int goalsInCareer;

    private int gretzkyOffset;

    public String template = "${action}! Game ${gameName}. ${playerName} scored ${scoredGoal} ${scoredTitle}${extra}. He has ${goalsInSeason} goals in season, ${goalsInCareer} goals in career and only ${gretzkyOffset} goals till Gretzky all-time record";

    public EventTemplate(EventData data) {
        this.data = data;
    }

    public String compile(String template) {
        if (!data.getScoredEnum().isEmpty()) {
            extra = ": " + String.join(", ", data.getScoredEnum());
        }

        goalsInSeason = data.getScoredBeforeCurrentSeason() + data.getScoredGoal();
        goalsInCareer = goalsInSeason + data.getScoredBeforeCurrentSeason();
        gretzkyOffset = scoreGretzky - (goalsInCareer);

        Map<String, String> arg = new LinkedHashMap<>();

        ObjectMapper om = new ObjectMapper();
        om.convertValue(this, new TypeReference<Map<String, Object>>() {
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
        om.convertValue(data, new TypeReference<Map<String, Object>>() {
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
        return TemplateTwix.template(template, arg, true);
    }

    @Override
    public String toString() {
        return compile(template);
    }

}
