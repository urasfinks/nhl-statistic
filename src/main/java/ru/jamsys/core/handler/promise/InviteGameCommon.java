package ru.jamsys.core.handler.promise;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.TelegramBotManager;
import ru.jamsys.core.flat.template.jdbc.DataMapper;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.flat.util.UtilNHL;
import ru.jamsys.core.jt.JTTeamScheduler;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;
import ru.jamsys.telegram.TelegramNotification;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

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
    }

    @Override
    public Promise generate() {
        return App.get(ServicePromise.class).get(getClass().getSimpleName(), 60_000L)
                .extension(promise -> promise.setRepositoryMapClass(Context.class, new Context()))
                .thenWithResource(
                        "insert",
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
                        })
                .then("handler", (_, _, promise) -> {
                    Context context = promise.getRepositoryMapClass(Context.class);
                    Set<TelegramNotification> map = new HashSet<>();
                    AtomicBoolean oviInGame = new AtomicBoolean(false);
                    context.getListInviteGame().forEach(row -> {
                        //{
                        //  "gameID" : "20250123_PHI@NYR",
                        //  "seasonType" : "Regular Season",
                        //  "away" : "PHI",
                        //  "gameTime" : "7:00p",
                        //  "teamIDHome" : "20",
                        //  "gameDate" : "20250123",
                        //  "gameStatus" : "Scheduled",
                        //  "gameTime_epoch" : "1737676800.0",
                        //  "teamIDAway" : "22",
                        //  "home" : "NYR",
                        //  "gameStatusCode" : "0",
                        //  "timeZone" : "-05:00",
                        //  "gameDateEpoch" : "20250124",
                        //  "gameDateTime" : "2025-01-23T19:00:00",
                        //  "gameDateTimeEpoch" : "2025-01-24T00:00:00",
                        //  "homeTeam" : "New York Rangers (NYR)",
                        //  "awayTeam" : "Philadelphia Flyers (PHI)",
                        //  "about" : "New York Rangers (NYR) vs Philadelphia Flyers (PHI)"
                        //}
                        if (UtilNHL.isOvi(row.idPlayer.toString())) {
                            oviInGame.set(true);
                        }
                        try {
                            Map<String, Object> mapOrThrow = UtilJson.getMapOrThrow(row.getJson());
                            String msg = String.format("""
                                            Матч %s 🆚 %s начнется уже через 12 часов — %s""",
                                    mapOrThrow.get("awayTeam"),
                                    mapOrThrow.get("homeTeam"),
                                    UtilNHL.formatDate(new BigDecimal(mapOrThrow.get("gameTime_epoch").toString()).longValue())
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

                    });
                    Util.logConsole(getClass(), "ovi: " + oviInGame.get());
                    Util.logConsoleJson(getClass(), map);
                })
                ;
    }

}
