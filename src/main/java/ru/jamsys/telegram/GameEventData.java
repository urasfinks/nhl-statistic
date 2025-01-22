package ru.jamsys.telegram;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.jamsys.tank.data.NHLPlayerList;

@Getter
@Setter
@Accessors(chain = true)
public class GameEventData {

    public enum Action {
        START_GAME,
        NOT_PLAY,
        GOAL,
        CANCEL,
        CORRECTION,
        FINISH_GAME
    }

    // Тип события
    private Action action;

    final private String gameAbout; // Описание игры X (ABV) vs Y (ABV)
    final private String gameScore; // Состояние игры с X 1 - 1 Y
    final private NHLPlayerList.Player player;
    final private String time; // Время события

    public GameEventData(Action action, String gameAbout, String gameScore, NHLPlayerList.Player player, String time) {
        this.action = action;
        this.gameAbout = gameAbout;
        this.gameScore = gameScore;
        this.player = player;
        this.time = time;
    }

    //Для событий игры
    private int scoredPrevGoal; // Кол-во голов в этом сезоне до текущего матча
    private int scoredLastSeason = 0; // Кол-во голов до этого сезона
    private int scoredGoal; // Кол-во голов в текущей игре

    // Для события конца игры
    private int scoredShots;  //🥅 Броски по воротам – shots
    private int scoredAssists;  //🏒 Передачи – assists
    private int scoredHits;  //🥷 Силовые приемы – hits
    private int scoredPenaltiesInMinutes;  //🥊 Штрафные минуты – penaltiesInMinutes
    private String scoredTimeOnIce;  //⏰ Время на льду – timeOnIce

    private boolean penaltyShot = false;
    private boolean overTime = false;

}
