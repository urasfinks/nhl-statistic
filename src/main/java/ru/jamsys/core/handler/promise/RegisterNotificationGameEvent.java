package ru.jamsys.core.handler.promise;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.TelegramBotManager;
import ru.jamsys.core.flat.util.UtilRisc;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.tank.data.NHLPlayerList;
import ru.jamsys.telegram.GameEventData;
import ru.jamsys.telegram.TelegramNotification;
import ru.jamsys.telegram.template.GameEventTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@Accessors(chain = true)
public class RegisterNotificationGameEvent implements PromiseGenerator {

    private final String log = RegisterNotificationGameEvent.class.getSimpleName();

    private final String idGame;

    private final NHLPlayerList.Player player;

    private final Set<GameEventData> listGameEventData = new HashSet<>();

    private Set<Long> listIdChat = new HashSet<>();

    public RegisterNotificationGameEvent(
            String idGame,
            NHLPlayerList.Player player
    ) {
        this.idGame = idGame;
        this.player = player;
    }

    @Override
    public Promise generate() {
        if (listIdChat == null || listIdChat.isEmpty()) {
            return null;
        }
        return App.get(ServicePromise.class).get(getClass().getSimpleName(), 60_000L)
                .then("lastGoals", new ScorePlayerCurrentSeasonBeforeGame(player, idGame).generate())
                .then("send", (atomicBoolean, _, promise) -> {
                    ScorePlayerCurrentSeasonBeforeGame stat = promise
                            .getRepositoryMapClass(Promise.class, "lastGoals")
                            .getRepositoryMapClass(ScorePlayerCurrentSeasonBeforeGame.class);
                    List<TelegramNotification> listEvent = new ArrayList<>();
                    List<TelegramNotification> listNotPlay = new ArrayList<>();
                    listGameEventData.forEach(gameEventData -> {
                        gameEventData.setScoredPrevGoal(stat.getCountGoal().get());
                        String message = new GameEventTemplate(gameEventData).toString();
                        TelegramBotManager telegramBotManager = App.get(TelegramBotManager.class);
                        UtilRisc.forEach(atomicBoolean, listIdChat, idChat -> {
                            TelegramNotification telegramNotification = new TelegramNotification(
                                    idChat,
                                    telegramBotManager.getCommonBotProperty().getName(),
                                    message,
                                    null,
                                    null
                            );
                            if (gameEventData.getAction().equals(GameEventData.Action.NOT_PLAY)) {
                                listNotPlay.add(telegramNotification);
                            } else {
                                listEvent.add(telegramNotification);
                            }
                        });
                    });
                    List<TelegramNotification> merge = new ArrayList<>();
                    merge.addAll(listNotPlay);
                    merge.addAll(listEvent);
                    RegisterNotification.add(merge);
                })
                .setDebug(false)
                ;
    }

}
