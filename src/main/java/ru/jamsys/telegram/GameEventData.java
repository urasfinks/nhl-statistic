package ru.jamsys.telegram;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Accessors(chain = true)
public class GameEventData {

    public enum Action {
        START_GAME,
        GOAL,
        CORRECTION,
        CANCEL,
        CANCEL_CORRECTION,
        FINISH_GAME
    }

    //action=GOAL, scoredTitle= goal, scoredGoal=1, scoredEnum=: 10:18 2nd period
    private Action action;
    private int scoredGoal;
    private List<String> scoredEnum = new ArrayList<>();
    private String playerName;
    private String gameName;

    private int scoredPrevGoalCurrentSeason; // Кол-во голов в этом сезоне до текущего матча
    private int scoredBeforeCurrentSeason = 0; // 853 - Овечкин Кол-во голов до этого сезона

}
