package ru.jamsys.core.handler.promise;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.DelaySenderComponent;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.TelegramBotComponent;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilFileResource;
import ru.jamsys.core.flat.util.UtilNHL;
import ru.jamsys.core.flat.util.UtilRisc;
import ru.jamsys.core.jt.JTOviSubscriber;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;
import ru.jamsys.tank.data.NHLGamesForPlayer;
import ru.jamsys.tank.data.NHLPlayerList;
import ru.jamsys.telegram.GameEventData;
import ru.jamsys.telegram.TelegramCommandContext;
import ru.jamsys.telegram.template.GameEventTemplateOvi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class SendNotificationGameEventOvi implements PromiseGenerator {

    private final String log = SendNotificationGameEventOvi.class.getSimpleName();

    private final String idGame;

    private final NHLPlayerList.Player player = UtilNHL.getOvi();

    private final GameEventData gameEventData;

    private final List<Long> listIdChat = new ArrayList<>();

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
                .thenWithResource("select", JdbcResource.class, (_, _, _, jdbcResource) -> {
                    List<Map<String, Object>> execute = jdbcResource.execute(new JdbcRequest(JTOviSubscriber.SELECT_ALL));
                    execute.forEach(map -> listIdChat.add(Long.parseLong(map.get("id_chat").toString())));
                })
                .then("lastGoals", new ScorePlayerCurrentSeasonBeforeGame(player, idGame).generate())
                .then("send", (atomicBoolean, _, promise) -> {
                    ScorePlayerCurrentSeasonBeforeGame stat = promise
                            .getRepositoryMapClass(Promise.class, "lastGoals")
                            .getRepositoryMapClass(ScorePlayerCurrentSeasonBeforeGame.class);
                    gameEventData
                            .setScoredPrevGoal(stat.getCountGoal().get());
                    String message = new GameEventTemplateOvi(gameEventData).toString();
                    TelegramBotComponent telegramBotComponent = App.get(TelegramBotComponent.class);

                    Util.logConsole("SEND TO CLIENT: " + message);

                    UtilRisc.forEach(atomicBoolean, listIdChat, idChat -> {
                        if (telegramBotComponent.getOviGoalsBot() != null) {
                            telegramBotComponent.getOviGoalsBot().send(idChat, message, null);
                        }
                    });
                    if (gameEventData.getAction().equals(GameEventData.Action.FINISH_GAME)) {
                        new HttpCacheReset(NHLGamesForPlayer.getUri(player.getPlayerID())).generate().run();
                    }
                    if (!gameEventData.getAction().equals(GameEventData.Action.FINISH_GAME)) {
                        promise.skipAllStep("not finish game");
                    }
                })
                .then("ovi", new PlayerStatistic(UtilNHL.getOvi(), UtilNHL.getOviScoreLastSeason()).generate())
                .then("send", (atomicBoolean, _, promise) -> {
                    PlayerStatistic ovi = promise.getRepositoryMapClass(Promise.class, "ovi")
                            .getRepositoryMapClass(PlayerStatistic.class);
                    String message = ovi.getMessage();
                    TelegramBotComponent telegramBotComponent = App.get(TelegramBotComponent.class);
                    UtilRisc.forEach(atomicBoolean, listIdChat, idChat -> {
                        App.get(DelaySenderComponent.class)
                                .add(
                                        new TelegramCommandContext()
                                                .setTelegramBot(telegramBotComponent.getOviGoalsBot())
                                                .setIdChat(idChat),
                                        message,
                                        null,
                                        null,
                                        10_000L
                                );

                        String pathImage = "image/" + ovi.getTotalGoals() + ".png";
                        if (
                                gameEventData.getScoredGoal() > 0
                                        && UtilFileResource.isFile(pathImage, UtilFileResource.Direction.PROJECT)
                        ) {
                            App.get(DelaySenderComponent.class)
                                    .add(
                                            new TelegramCommandContext()
                                                    .setTelegramBot(telegramBotComponent.getOviGoalsBot())
                                                    .setIdChat(idChat),
                                            null,
                                            null,
                                            pathImage,
                                            15_000L
                                    );
                        }
                    });
                })
                .setDebug(false)
                ;
    }

}
