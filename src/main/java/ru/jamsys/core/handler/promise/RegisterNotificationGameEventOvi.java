package ru.jamsys.core.handler.promise;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.TelegramBotManager;
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
import ru.jamsys.telegram.TelegramNotification;
import ru.jamsys.telegram.template.GameEventTemplateOvi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

// Изначально предполагалось, что это обещание будет обрабатывать одно событие по игроку
// В случае с Ovi добавилась пробежка по всем подписантам Ovi и получается событие множилось
// Потом пришла рассылка с конца очереди, и пришлось менять порядок вставки, но тут одно событие

@Getter
@Setter
public class RegisterNotificationGameEventOvi implements PromiseGenerator {

    private final String log = RegisterNotificationGameEventOvi.class.getSimpleName();

    private final String idGame;

    private final NHLPlayerList.Player player = UtilNHL.getOvi();

    private final List<GameEventData> listGameEventData = new ArrayList<>();

    private final List<Long> listIdChat = new ArrayList<>();

    private final List<TelegramNotification> listNotPlay = new ArrayList<>();

    private final List<TelegramNotification> listEvent = new ArrayList<>();

    public RegisterNotificationGameEventOvi(
            String idGame
    ) {
        this.idGame = idGame;
    }

    @Override
    public Promise generate() {
        return App.get(ServicePromise.class).get(getClass().getSimpleName(), 600_000L)
                .thenWithResource("select", JdbcResource.class, (_, _, _, jdbcResource) -> {
                    List<Map<String, Object>> execute = jdbcResource.execute(new JdbcRequest(JTOviSubscriber.SELECT_NOT_REMOVE));
                    execute.forEach(map -> listIdChat.add(Long.parseLong(map.get("id_chat").toString())));
                })
                .then("lastGoals", new ScorePlayerCurrentSeasonBeforeGame(player, idGame).generate())
                .then("send", (atomicBoolean, _, promise) -> {
                    ScorePlayerCurrentSeasonBeforeGame stat = promise
                            .getRepositoryMapClass(Promise.class, "lastGoals")
                            .getRepositoryMapClass(ScorePlayerCurrentSeasonBeforeGame.class);
                    AtomicBoolean gameInProcess = new AtomicBoolean(true);
                    listGameEventData.forEach(gameEventData -> {
                        gameEventData.setScoredPrevGoal(stat.getCountGoal().get());
                        String message = new GameEventTemplateOvi(gameEventData).toString();
                        String botName = App.get(TelegramBotManager.class).getOviBotProperty().getName();
                        UtilRisc.forEach(atomicBoolean, listIdChat, idChat -> {
                            if (gameEventData.getAction().equals(GameEventData.Action.NOT_PLAY)) {
                                listNotPlay.add(new TelegramNotification(
                                        idChat,
                                        botName,
                                        message,
                                        null,
                                        null
                                ));
                            } else {
                                listEvent.add(new TelegramNotification(
                                        idChat,
                                        botName,
                                        message,
                                        null,
                                        null
                                ));
                            }
                            if (gameEventData.getAction().equals(GameEventData.Action.FINISH_GAME)) {
                                new HttpCacheReset(NHLGamesForPlayer.getUri(player.getPlayerID())).generate().run();
                            }
                            if (gameEventData.getAction().equals(GameEventData.Action.FINISH_GAME)) {
                                gameInProcess.set(false);
                            }
                        });
                    });
                    if (gameInProcess.get()) {
                        ArrayList<TelegramNotification> merge = new ArrayList<>();
                        merge.addAll(listNotPlay);
                        merge.addAll(listEvent);
                        RegisterNotification.add(merge);
                        promise.skipAllStep("Game in process");
                    }
                })
                .then("ovi", new PlayerStatistic(UtilNHL.getOvi(), UtilNHL.getOviScoreLastSeason()).generate())
                .then("send", (atomicBoolean, _, promise) -> {
                    PlayerStatistic ovi = promise.getRepositoryMapClass(Promise.class, "ovi")
                            .getRepositoryMapClass(PlayerStatistic.class);
                    String message = ovi.getMessage();
                    TelegramBotManager telegramBotComponent = App.get(TelegramBotManager.class);
                    List<TelegramNotification> listSendStat = new ArrayList<>();
                    List<TelegramNotification> listSendImage = new ArrayList<>();
                    UtilRisc.forEach(atomicBoolean, listIdChat, idChat -> {
                        listSendStat.add(new TelegramNotification(
                                idChat,
                                telegramBotComponent.getOviBotProperty().getName(),
                                message,
                                null,
                                null
                        ));

                        String pathImage = "image/" + ovi.getTotalGoals() + ".png";
                        if (
                                listGameEventData.getFirst().getScoredGoal() > 0
                                        && UtilFileResource.isFile(pathImage, UtilFileResource.Direction.PROJECT)
                        ) {
                            listSendImage.add(new TelegramNotification(
                                    idChat,
                                    telegramBotComponent.getOviBotProperty().getName(),
                                    null,
                                    null,
                                    pathImage
                            ));
                        }
                    });
                    List<TelegramNotification> merge = new ArrayList<>();
                    merge.addAll(listSendImage);
                    merge.addAll(listSendStat);
                    merge.addAll(listNotPlay);
                    merge.addAll(listEvent);
                    RegisterNotification.add(merge);
                })
                .setDebug(false)
                ;
    }

}
