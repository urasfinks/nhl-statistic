package ru.jamsys.telegram;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

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

    private Action action;
    private int scoredGoal;
    private String playerName;
    private String gameName;
    private String teamsScore;
    private String time;

    private int scoredPrevGoal; // Кол-во голов в этом сезоне до текущего матча
    private int scoredLastSeason = 0; // Кол-во голов до этого сезона

    // Для события конца игры
    private int scoredShots;  //🥅 Броски по воротам – shots
    private int scoredAssists;  //🏒 Передачи – assists
    private int scoredHits;  //🥷 Силовые приемы – hits
    private int scoredPenaltiesInMinutes;  //🥊 Штрафные минуты – penaltiesInMinutes
    private String scoredTimeOnIce;  //⏰ Время на льду – timeOnIce

}
