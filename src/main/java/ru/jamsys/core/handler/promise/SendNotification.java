package ru.jamsys.core.handler.promise;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.TelegramBotComponent;
import ru.jamsys.core.flat.util.UtilRisc;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.tank.data.NHLPlayerList;
import ru.jamsys.telegram.NotificationDataAndTemplate;

import java.util.List;

@Getter
@Setter
public class SendNotification implements PromiseGenerator {

    private final String idGame;

    private final NHLPlayerList.Player player;

    private final NotificationDataAndTemplate notificationDataAndTemplate;

    private final List<Integer> listIdChat;

    public SendNotification(
            String idGame,
            NHLPlayerList.Player player,
            NotificationDataAndTemplate notificationDataAndTemplate,
            List<Integer> listIdChat
    ) {
        this.idGame = idGame;
        this.player = player;
        this.notificationDataAndTemplate = notificationDataAndTemplate;
        this.listIdChat = listIdChat;
    }

    @Override
    public Promise generate() {
        if (listIdChat == null || listIdChat.isEmpty()) {
            return null;
        }
        return App.get(ServicePromise.class).get(getClass().getSimpleName(), 60_000L)
                .then("lastGoals", new GetPlayerScoreCurrentSeason(idGame, player).generate())
                .then("send", (atomicBoolean, _, promise) -> {
                    String prevGoal = promise.getRepositoryMapClass(Promise.class, "lastGoals").getRepositoryMap(String.class, "prev_goal", "0");
                    NotificationDataAndTemplate notificationDataAndTemplate = new NotificationDataAndTemplate()
                            .setPlayerName(NHLPlayerList.getPlayerName(player))
                            .setGameName(idGame.substring(idGame.indexOf("_") + 1))
                            .setScoredPrevGoalCurrentSeason(Integer.parseInt(prevGoal));
                    String message = notificationDataAndTemplate.toString();
                    TelegramBotComponent telegramBotComponent = App.get(TelegramBotComponent.class);
                    System.out.println("SEND TO CLIENT: " + message);
                    UtilRisc.forEach(atomicBoolean, listIdChat, idChat -> {
                        if (telegramBotComponent.getHandler() != null) {
                            telegramBotComponent.getHandler().send(idChat, message, null);
                        }
                    });
                })
                .setDebug(true)
                ;
    }

}
