package ru.jamsys.telegram;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.jamsys.core.flat.template.twix.TemplateTwix;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Accessors(chain = true)
public class NotificationDataAndTemplate {

    //action=GOAL, scoredTitle= goal, scoredGoal=1, scoredEnum=: 10:18 2nd period
    private String action;
    private String scoredTitle;
    private int scoredGoal;
    private List<String> scoredEnum = new ArrayList<>();
    private String playerName;
    private String gameName;

    private String extra = "";

    private int scoredPrevGoalCurrentSeason; // Кол-во голов в этом сезоне до текущего матча
    private int scoredBeforeCurrentSeason = 0; // 853 - Овечкин Кол-во голов до этого сезона
    private int scoreGretzky = 894; // Кол-во голов у Gretzky

    private int goalsInSeason;
    private int goalsInCareer;
    private int gretzkyOffset;

    public String getDefaultTemplate() {
        return "${action}! Game ${gameName}. ${playerName} scored ${scoredGoal} ${scoredTitle}${extra}. He has ${goalsInSeason} goals in season, ${goalsInCareer} goals in career and only ${gretzkyOffset} goals till Gretzky all-time record";
    }

    @Override
    public String toString() {
        return compile(getDefaultTemplate());
    }

    public String compile(String template) {
        if (!scoredEnum.isEmpty()) {
            extra = ": " + String.join(", ", scoredEnum);
        }
        goalsInSeason = scoredPrevGoalCurrentSeason + scoredGoal;
        goalsInCareer = goalsInSeason + scoredBeforeCurrentSeason;
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
        return TemplateTwix.template(template, arg, true);
    }

}
