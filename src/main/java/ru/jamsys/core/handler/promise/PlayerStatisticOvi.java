package ru.jamsys.core.handler.promise;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.flat.util.UtilNHL;

@Getter
@Setter
public class PlayerStatisticOvi extends PlayerStatistic {

    public PlayerStatisticOvi() {
        super(UtilNHL.getOvi(), UtilNHL.getOviScoreLastSeason());
    }

}
