package ru.jamsys.telegram;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Accessors(chain = true)
public class EventData {

    //action=GOAL, scoredTitle= goal, scoredGoal=1, scoredEnum=: 10:18 2nd period
    private String action;
    private String scoredTitle;
    private int scoredGoal;
    private List<String> scoredEnum = new ArrayList<>();
    private String playerName;
    private String gameName;

    private int scoredPrevGoalCurrentSeason; // Кол-во голов в этом сезоне до текущего матча
    private int scoredBeforeCurrentSeason = 0; // 853 - Овечкин Кол-во голов до этого сезона

}
