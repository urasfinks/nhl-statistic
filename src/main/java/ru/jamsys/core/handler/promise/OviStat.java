package ru.jamsys.core.handler.promise;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.tank.data.NHLPlayerList;

@Getter
@Setter
public class OviStat extends PlayerStat {

    public OviStat() {
        super(new NHLPlayerList.Player()
                        .setPlayerID("3101")
                        .setPos("LW")
                        .setLongName("Alex Ovechkin")
                        .setTeam("WSH")
                        .setTeamID("31"),
                853
        );
    }

}
