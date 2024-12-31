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
        CANCEL,
        FINISH_GAME
    }

    //action=GOAL, scoredTitle= goal, scoredGoal=1, scoredEnum=: 10:18 2nd period
    private Action action;
    private int scoredGoal;
    private List<String> scoredEnum = new ArrayList<>();
    private String playerName;
    private String gameName;
    private String teamsScore;

    private int scoredPrevGoal; // Кол-во голов в этом сезоне до текущего матча
    private int scoredLastSeason = 0; // Кол-во голов до этого сезона

}
