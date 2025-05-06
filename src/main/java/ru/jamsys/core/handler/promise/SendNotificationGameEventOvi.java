package ru.jamsys.core.handler.promise;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.TelegramBotManager;
import ru.jamsys.core.flat.util.*;
import ru.jamsys.core.jt.JTOviSubscriber;
import ru.jamsys.core.jt.JTVote;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

// Изначально предполагалось, что это обещание будет обрабатывать одно событие по игроку
// В случае с Ovi добавилась пробежка по всем подписантам Ovi и получается событие множилось
// Потом пришла рассылка с конца очереди, и пришлось менять порядок вставки, но тут одно событие

@Getter
@Setter
public class SendNotificationGameEventOvi implements PromiseGenerator {

    private final String log = SendNotificationGameEventOvi.class.getSimpleName();

    private final String idGame;

    private final NHLPlayerList.Player player = UtilNHL.getOvi();

    private final List<GameEventData> listGameEventData = new ArrayList<>();

    private final List<Long> listIdChat = new ArrayList<>();

    private final List<TelegramNotification> listEvent = new ArrayList<>();

    private final Map<Long, Boolean> userVote = new HashMap<>(); // key - idChat; value - vote

    private Integer scoredGoalForward = null;

    public SendNotificationGameEventOvi(
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
                            listEvent.add(new TelegramNotification(
                                    idChat,
                                    botName,
                                    message,
                                    null,
                                    null
                            ));
                        });
                        if (gameEventData.getAction().equals(GameEventData.Action.FINISH_GAME)) {
                            new HttpCacheReset(NHLGamesForPlayer.getUri(player.getPlayerID())).generate().run();
                            gameInProcess.set(false);
                            try {
                                scoredGoalForward = gameEventData.getScoredGoal();
                            } catch (Throwable th) {
                                App.error(th);
                            }
                        }
                    });
                    if (gameInProcess.get()) {
                        RegisterNotification.add(listEvent);
                        promise.skipAllStep("Game in process");
                    }
                })
                .then("ovi", new PlayerStatistic(UtilNHL.getOvi(), UtilNHL.getOviScoreLastSeason()).generate())
                .thenWithResource("selectVote", JdbcResource.class, (_, _, _, jdbcResource) -> jdbcResource
                        .execute(new JdbcRequest(JTVote.SELECT_VOTE_GAME)
                                        .addArg("id_game", idGame)
                                        .addArg("id_player", player.getPlayerID()),
                                JTVote.Row.class
                        ).forEach(row -> getUserVote().put(
                                Long.parseLong(row.getIdChat().toString()),
                                "true".equals(row.getVote())
                        ))
                )
                .then("send", (atomicBoolean, _, _) -> {
                    String botName = App.get(TelegramBotManager.class).getOviBotProperty().getName();
                    List<TelegramNotification> merge = new ArrayList<>();
                    // Боялся повлиять на общую рассылку, поэтому всё оборачивал в try, так как на живую тестировали
                    try {
                        if (scoredGoalForward != null) {
                            UtilRisc.forEach(atomicBoolean, listIdChat, idChat -> {
                                if (getUserVote().containsKey(idChat)) {
                                    merge.add(new TelegramNotification(
                                            idChat,
                                            botName,
                                            UtilVoteOvi.get(scoredGoalForward, getUserVote().get(idChat)),
                                            null,
                                            null
                                    ));
                                }
                            });
                        }
                    } catch (Throwable th) {
                        App.error(th);
                    }
                    merge.addAll(listEvent);
                    RegisterNotification.add(merge);
                })
                .setDebug(false)
                ;
    }

}
