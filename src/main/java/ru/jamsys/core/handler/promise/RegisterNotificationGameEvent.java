package ru.jamsys.core.handler.promise;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.RegisterDelayNotification;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.TelegramBotComponent;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilRisc;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.tank.data.NHLPlayerList;
import ru.jamsys.telegram.GameEventData;
import ru.jamsys.telegram.NotificationObject;
import ru.jamsys.telegram.template.GameEventTemplate;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class RegisterNotificationGameEvent implements PromiseGenerator {

    private final String log = RegisterNotificationGameEvent.class.getSimpleName();

    private final String idGame;

    private final NHLPlayerList.Player player;

    private final GameEventData gameEventData;

    private final List<Long> listIdChat;

    public RegisterNotificationGameEvent(
            String idGame,
            NHLPlayerList.Player player,
            GameEventData gameEventData,
            List<Long> listIdChat
    ) {
        this.idGame = idGame;
        this.player = player;
        this.gameEventData = gameEventData;
        this.listIdChat = listIdChat;
    }

    @Override
    public Promise generate() {
        if (listIdChat == null || listIdChat.isEmpty()) {
            return null;
        }
        return App.get(ServicePromise.class).get(getClass().getSimpleName(), 60_000L)
                .then("lastGoals", new ScorePlayerCurrentSeasonBeforeGame(player, idGame).generate())
                .then("send", (atomicBoolean, _, promise) -> {
                    String prevGoal = promise.getRepositoryMapClass(Promise.class, "lastGoals").getRepositoryMap(String.class, "prev_goal", "0");
                    gameEventData
                            .setScoredPrevGoal(Integer.parseInt(prevGoal));
                    String message = new GameEventTemplate(gameEventData).toString();
                    TelegramBotComponent telegramBotComponent = App.get(TelegramBotComponent.class);

                    List<NotificationObject> listEvent = new ArrayList<>();
                    List<NotificationObject> listNotPlay = new ArrayList<>();

                    UtilRisc.forEach(atomicBoolean, listIdChat, idChat -> {
                        NotificationObject notificationObject = new NotificationObject(
                                idChat,
                                telegramBotComponent.getNhlStatisticsBot().getBotUsername(),
                                message,
                                null,
                                null
                        );
                        if (gameEventData.getAction().equals(GameEventData.Action.NOT_PLAY)) {
                            listNotPlay.add(notificationObject);
                        } else {
                            listEvent.add(notificationObject);
                        }
                    });
                    RegisterDelayNotification.add(listNotPlay, 10_000L);
                    RegisterNotification.add(listEvent);
                })
                .setDebug(false)
                ;
    }

}
