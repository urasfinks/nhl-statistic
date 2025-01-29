package ru.jamsys.core.handler.promise;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.component.TelegramBotManager;
import ru.jamsys.core.extension.builder.ArrayListBuilder;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.http.ServletResponseWriter;
import ru.jamsys.core.flat.util.UtilDate;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.flat.util.UtilNHL;
import ru.jamsys.core.flat.util.UtilRisc;
import ru.jamsys.core.flat.util.telegram.Button;
import ru.jamsys.core.jt.JTOviSubscriber;
import ru.jamsys.core.jt.JTTeamScheduler;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;
import ru.jamsys.telegram.TelegramNotification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Accessors(chain = true)
public class InviteGameOvi implements PromiseGenerator {

    private final JTTeamScheduler.RowInviteGame oviInviteGame;

    public InviteGameOvi(JTTeamScheduler.RowInviteGame oviInviteGame) {
        this.oviInviteGame = oviInviteGame;
    }

    @Getter
    @Setter
    public static class Context {
        private final List<Long> listIdChat = new ArrayList<>();
        private final List<TelegramNotification> list = new ArrayList<>();
    }

    @Override
    public Promise generate() {
        return App.get(ServicePromise.class).get(getClass().getSimpleName(), 60_000L)
                .extension(promise -> promise.setRepositoryMapClass(Context.class, new Context()))
                .then("check", (_, _, promise) -> {
                    if (oviInviteGame == null) {
                        promise.skipAllStep("oviInviteGame is null");
                        return;
                    }
                    ServiceProperty serviceProperty = App.get(ServiceProperty.class);
                    String mode = serviceProperty.get(String.class, "run.mode", "test");
                    if (!mode.equals("prod")) {
                        promise.skipAllStep("mode not prod");
                    }
                })
                .thenWithResource("select", JdbcResource.class, (_, _, promise, jdbcResource) -> {
                    Context context = promise.getRepositoryMapClass(Context.class);
                    List<Map<String, Object>> execute = jdbcResource.execute(new JdbcRequest(JTOviSubscriber.SELECT_NOT_REMOVE));
                    execute.forEach(map -> context.getListIdChat().add(Long.parseLong(map.get("id_chat").toString())));
                })
                .then("send", (atomicBoolean, _, promise) -> {
                    Context context = promise.getRepositoryMapClass(Context.class);
                    Map<String, Object> mapOrThrow = UtilJson.getMapOrThrow(oviInviteGame.getJson());
                    long timeGame = new BigDecimal(mapOrThrow.get("gameTime_epoch").toString()).longValue();
                    String msg = String.format("""
                                    Матч %s 🆚 %s начнется уже через %s — %s
                                    
                                    Как думаешь, сможет ли Александр Овечкин забить сегодня?
                                    
                                    """,
                            mapOrThrow.get("awayTeam"),
                            mapOrThrow.get("homeTeam"),
                            UtilDate.getTimeBetween(System.currentTimeMillis(), timeGame * 1000).getDescription(
                                    2,
                                    UtilDate.TimeBetween.StyleDescription.FORMAL
                            ),
                            UtilNHL.formatDate(timeGame)
                    );
                    List<Button> listButtons = new ArrayListBuilder<Button>()
                            .append(new Button(
                                    "Да 🔥",
                                    ServletResponseWriter.buildUrlQuery(
                                            "/vote/",
                                            new HashMapBuilder<String, String>()
                                                    .append("g", oviInviteGame.getIdGame())
                                                    .append("p", oviInviteGame.getIdPlayer().toString())
                                                    .append("v", "true")

                                    )
                            ))
                            .append(new Button(
                                    "Нет ⛔",
                                    ServletResponseWriter.buildUrlQuery(
                                            "/vote/",
                                            new HashMapBuilder<String, String>()
                                                    .append("g", oviInviteGame.getIdGame())
                                                    .append("p", oviInviteGame.getIdPlayer().toString())
                                                    .append("v", "false")

                                    )
                            ));

                    UtilRisc.forEach(atomicBoolean, context.getListIdChat(), idChat -> {
                        context.getList().add(new TelegramNotification(
                                idChat,
                                App.get(TelegramBotManager.class).getOviBotProperty().getName(),
                                msg,
                                listButtons,
                                null
                        ));
                    });
                })
                .then("send", (_, _, promise) -> {
                    Context context = promise.getRepositoryMapClass(Context.class);
                    RegisterNotification.add(context.getList());
                })
                .setDebug(false)
                ;
    }

}
