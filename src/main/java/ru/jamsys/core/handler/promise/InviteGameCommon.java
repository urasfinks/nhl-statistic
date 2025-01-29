package ru.jamsys.core.handler.promise;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.component.TelegramBotManager;
import ru.jamsys.core.extension.builder.HashMapBuilder;
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
    public static class Context {
        private List<JTTeamScheduler.RowInviteGame> listInviteGame = new ArrayList<>();
        private Set<String> listIdGames = new HashSet<>();
        private Set<TelegramNotification> uniqueNotification = new HashSet<>();
        private JTTeamScheduler.RowInviteGame oviInviteGame = null;
    }

    @Override
    public Promise generate() {
        return App.get(ServicePromise.class).get(getClass().getSimpleName(), 60_000L)
                .extension(promise -> promise.setRepositoryMapClass(Context.class, new Context()))
                .then("check", (_, _, promise) -> {
                    ServiceProperty serviceProperty = App.get(ServiceProperty.class);
                    String mode = serviceProperty.get(String.class, "run.mode", "test");
                    if (!mode.equals("prod")) {
                        promise.skipAllStep("mode not prod");
                    }
                })
                .thenWithResource(
                        "select",
                        JdbcResource.class,
                        (_, _, promise, jdbcResource) -> {
                            Context context = promise.getRepositoryMapClass(Context.class);
                            context.setListInviteGame(jdbcResource.execute(
                                    new JdbcRequest(JTTeamScheduler.SELECT_INVITE_GAME),
                                    JTTeamScheduler.RowInviteGame.class
                            ));
                            if (context.getListInviteGame().isEmpty()) {
                                promise.skipAllStep("inviteGame is empty");
                            }
                        }
                )
                .then("handler", (_, _, promise) -> {
                    Context context = promise.getRepositoryMapClass(Context.class);
                    for (JTTeamScheduler.RowInviteGame rowInviteGame : context.getListInviteGame()) {
                        if (UtilNHL.isOvi(rowInviteGame.getIdPlayer().toString())) {
                            context.setOviInviteGame(rowInviteGame);
                        }
                        context.getListIdGames().add(rowInviteGame.getIdGame());
                        try {
                            Map<String, Object> mapOrThrow = UtilJson.getMapOrThrow(rowInviteGame.getJson());
                            long timeGame = new BigDecimal(mapOrThrow.get("gameTime_epoch").toString()).longValue();
                            Util.logConsoleJson(getClass(), new HashMapBuilder<>(mapOrThrow)
                                    .append("curTimestamp", System.currentTimeMillis())
                                    .append("gameTimestamp", timeGame * 1000)
                                    .append("formal", UtilDate.getTimeBetween(System.currentTimeMillis(), timeGame * 1000).getDescription(
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
                            context
                                    .getUniqueNotification()
                                    .add(new TelegramNotification(
                                            rowInviteGame.getIdChat().longValue(),
                                            App.get(TelegramBotManager.class).getCommonBotProperty().getName(),
                                            msg,
                                            null,
                                            null
                                    ));

                        } catch (Throwable th) {
                            App.error(th);
                        }
                    }
                    if (context.getListIdGames().isEmpty()) {
                        promise.skipAllStep("listIdGames is empty");
                    }
                })
                .then("send", (_, _, promise) -> {
                    Context context = promise.getRepositoryMapClass(Context.class);
                    RegisterNotificationTest.add(context.getUniqueNotification().stream().toList());
                    if (context.getOviInviteGame() != null) {
                        new InviteGameOvi(context.getOviInviteGame()).generate().run();
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
