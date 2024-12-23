package ru.jamsys.core.handler.promise;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
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

    private final NotificationDataAndTemplate template;

    private final List<Integer> listIdChat;

    public SendNotification(String idGame, NHLPlayerList.Player player, NotificationDataAndTemplate template, List<Integer> listIdChat) {
        this.idGame = idGame;
        this.player = player;
        this.template = template;
        this.listIdChat = listIdChat;
    }

    @Override
    public Promise generate() {
        return App.get(ServicePromise.class).get(getClass().getSimpleName(), 60_000L)
                .then("lastGoals", new GetPlayerScoreCurrentSeason(idGame, player).generate())
                .then("send", (atomicBoolean, promiseTask, promise) -> {
//                    String message = TemplateTwix.template(
//                            template,
//                            new HashMapBuilder<String, String>()
//                                    .append("playerName", NHLPlayerList.getPlayerName(player))
//                                    .append("gameName", idGame.substring(idGame.indexOf("_") + 1))
//                    );
                    //System.out.println("SEND TO CLIENT: " + message);
                    UtilRisc.forEach(atomicBoolean, listIdChat, idChat -> {
//                        if (telegramBotComponent.getHandler() != null) {
//                            telegramBotComponent.getHandler().send(idChat, message, null);
//                        }
                    });
                })
                .setDebug(true)
                ;
    }

}
