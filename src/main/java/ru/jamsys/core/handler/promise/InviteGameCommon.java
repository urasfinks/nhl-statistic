package ru.jamsys.core.handler.promise;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.TelegramBotManager;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.flat.template.jdbc.DataMapper;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilDate;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.flat.util.UtilNHL;
import ru.jamsys.core.jt.JTTeamScheduler;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;
import ru.jamsys.telegram.TelegramNotification;

import java.math.BigDecimal;
import java.util.*;

@Accessors(chain = true)
public class InviteGameCommon implements PromiseGenerator {

    @Getter
    @Setter
    public static class Row extends DataMapper<Row> {
        BigDecimal idChat;
        BigDecimal idPlayer;
        String idGame;
        String json;
    }

    @Getter
    @Setter
    public static class Context {
        List<Row> listInviteGame;
        List<String> listIdGames = new ArrayList<>();
    }

    @Override
    public Promise generate() {
        return App.get(ServicePromise.class).get(getClass().getSimpleName(), 60_000L)
                .extension(promise -> promise.setRepositoryMapClass(Context.class, new Context()))
                .thenWithResource(
                        "select",
                        JdbcResource.class,
                        (_, _, promise, jdbcResource) -> {
                            Context context = promise.getRepositoryMapClass(Context.class);
                            context.setListInviteGame(jdbcResource.execute(
                                    new JdbcRequest(JTTeamScheduler.SELECT_INVITE_GAME),
                                    Row.class
                            ));
                            if (context.getListInviteGame().isEmpty()) {
                                promise.skipAllStep("inviteGame is empty");
                            }
                        }
                )
                .then("handler", (_, _, promise) -> {
                    Context context = promise.getRepositoryMapClass(Context.class);
                    Row oviInviteGame = null;
                    Set<TelegramNotification> map = new HashSet<>();
                    for (Row row : context.getListInviteGame()) {
                        if (UtilNHL.isOvi(row.idPlayer.toString())) {
                            oviInviteGame = row;
                        }
                        try {
                            Map<String, Object> mapOrThrow = UtilJson.getMapOrThrow(row.getJson());
                            if (mapOrThrow.containsKey("gameID")) {
                                context.getListIdGames().add(mapOrThrow.get("gameID").toString());
                            }
                            long timeGame = new BigDecimal(mapOrThrow.get("gameTime_epoch").toString()).longValue();
                            Util.logConsoleJson(getClass(), new HashMapBuilder<String, Object>(mapOrThrow)
                                    .append("curTimestamp", System.currentTimeMillis())
                                    .append("gameTimestamp", timeGame * 1000)
                                    .append("fomal", UtilDate.getTimeBetween(System.currentTimeMillis(), timeGame * 1000).getDescription(
                                            2,
                                            UtilDate.TimeBetween.StyleDescription.FORMAL
                                    ))
                            );
                            String msg = String.format("""
                                            Матч %s 🆚 %s начнется уже через %s — %s""",
                                    mapOrThrow.get("awayTeam"),
                                    mapOrThrow.get("homeTeam"),
                                    UtilDate.getTimeBetween(System.currentTimeMillis(), timeGame * 1000).getDescription(
                                            2,
                                            UtilDate.TimeBetween.StyleDescription.FORMAL
                                    ),
                                    UtilNHL.formatDate(timeGame)
                            );
                            map
                                    .add(new TelegramNotification(
                                            row.getIdChat().longValue(),
                                            App.get(TelegramBotManager.class).getCommonBotProperty().getName(),
                                            msg,
                                            null,
                                            null
                                    ));

                        } catch (Throwable th) {
                            App.error(th);
                        }
                    }
                    RegisterNotificationTest.add(map.stream().toList());
                    if (context.getListIdGames().isEmpty()) {
                        promise.skipAllStep("listIdGames is empty");
                    }
                })
                .thenWithResource(
                        "update",
                        JdbcResource.class,
                        (_, _, promise, jdbcResource) -> {
                            Context context = promise.getRepositoryMapClass(Context.class);
                            jdbcResource.execute(
                                    new JdbcRequest(JTTeamScheduler.UPDATE_INVITED_GAME)
                                            .addArg("id_game", context.getListIdGames())
                            );
                        }
                )
                .setDebug(false)
                ;
    }

}
