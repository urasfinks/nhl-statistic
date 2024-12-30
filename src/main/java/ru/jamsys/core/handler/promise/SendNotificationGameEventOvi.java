package ru.jamsys.core.handler.promise;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.TelegramBotComponent;
import ru.jamsys.core.flat.util.UtilNHL;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.tank.data.NHLGamesForPlayer;
import ru.jamsys.tank.data.NHLPlayerList;
import ru.jamsys.telegram.GameEventData;
import ru.jamsys.telegram.template.GameEventTemplateOvi;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class SendNotificationGameEventOvi implements PromiseGenerator {

    private final String idGame;

    private final NHLPlayerList.Player player = UtilNHL.getOvi();

    private final GameEventData gameEventData;

    private final List<Integer> listIdChat = new ArrayList<>();

    public SendNotificationGameEventOvi(
            String idGame,
            GameEventData gameEventData
    ) {
        this.idGame = idGame;
        this.gameEventData = gameEventData;
    }

    @Override
    public Promise generate() {
        return App.get(ServicePromise.class).get(getClass().getSimpleName(), 60_000L)
                .then("lastGoals", new ScorePlayerCurrentSeasonBeforeGame(player, idGame).generate())
                .then("send", (atomicBoolean, _, promise) -> {
                    String prevGoal = promise.getRepositoryMapClass(Promise.class, "lastGoals").getRepositoryMap(String.class, "prev_goal", "0");
                    gameEventData
                            .setPlayerName(NHLPlayerList.getPlayerName(player))
                            .setGameName(idGame.substring(idGame.indexOf("_") + 1))
                            .setScoredPrevGoal(Integer.parseInt(prevGoal));
                    String message = new GameEventTemplateOvi(gameEventData).toString();
                    TelegramBotComponent telegramBotComponent = App.get(TelegramBotComponent.class);
                    System.out.println("SEND TO CLIENT: " + message);
//                    UtilRisc.forEach(atomicBoolean, listIdChat, idChat -> {
//                        if (telegramBotComponent.getNhlStatisticsBot() != null) {
//                            telegramBotComponent.getNhlStatisticsBot().send(idChat, message, null);
//                        }
//                    });
                    if (gameEventData.getAction().equals(GameEventData.Action.FINISH_GAME)) {
                        new HttpCacheReset(NHLGamesForPlayer.getUri(player.getPlayerID())).generate().run();
                    }
                })
                .setDebug(false)
                ;
    }

}
