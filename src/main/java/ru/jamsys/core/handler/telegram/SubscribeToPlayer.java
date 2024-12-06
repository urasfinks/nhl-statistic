package ru.jamsys.core.handler.telegram;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.http.ServletResponseWriter;
import ru.jamsys.core.flat.util.UtilDate;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.flat.util.UtilTelegram;
import ru.jamsys.core.flat.util.tank.UtilTank01;
import ru.jamsys.core.flat.util.telegram.Button;
import ru.jamsys.core.jt.JTScheduler;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;
import ru.jamsys.tank.data.NHLPlayerList;
import ru.jamsys.tank.data.NHLTeamSchedule;
import ru.jamsys.telegram.TelegramCommandContext;
import ru.jamsys.telegram.TelegramCommandHandler;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

@Setter
@Getter
@Component
@RequestMapping("/subscribe_to_player/**")
public class SubscribeToPlayer implements PromiseGenerator, TelegramCommandHandler {

    private String index;

    private final ServicePromise servicePromise;

    public SubscribeToPlayer(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(index, 12_000L)
                .then("check", (_, _, promise) -> {
                    TelegramCommandContext context = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    System.out.println(UtilJson.toStringPretty(context, "{}"));
                    if (!context.getUriParameters().containsKey("namePlayer")) {
                        context.getStepHandler().put(context.getIdChat(), context.getUriPath() + "/?namePlayer=");
                        context.getTelegramBot().send(
                                context.getIdChat(),
                                "Enter the player's name:",
                                null
                        );
                        promise.skipAllStep();
                        return;
                    }
                    if (context.getUriParameters().containsKey("idPlayer")) {
                        promise.goTo("findPlayerByIdMarker");
                    }
                })
                .extension(NHLPlayerList::promiseExtensionGetPlayerList)
                .then("findPlayerByName", (_, _, promise) -> {
                    UtilTank01.Response response = promise.getRepositoryMapClass(UtilTank01.Response.class);
                    TelegramCommandContext context = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    List<Map<String, Object>> userList = NHLPlayerList.findByName(
                            context.getUriParameters().get("namePlayer"),
                            response.getData()
                    );
                    if (userList.isEmpty()) {
                        context.getTelegramBot().send(context.getIdChat(), "Player's not found", null);
                        return;
                    }
                    int counter = 0;
                    List<Button> buttons = new ArrayList<>();
                    for (Map<String, Object> player : userList) {
                        buttons.add(new Button(
                                player.get("longName").toString() + " (" + player.get("team").toString() + ")",
                                ServletResponseWriter.buildUrlQuery(
                                        context.getUriPath() + "/",
                                        new HashMapBuilder<>(context.getUriParameters())
                                                .append("idPlayer", player.get("playerID").toString())
                                )
                        ));
                        counter++;
                        if (counter > 2) {
                            break;
                        }
                    }
                    context.getTelegramBot().send(context.getIdChat(), "Choose Player:", buttons);
                    promise.skipAllStep();
                })
                .then("findPlayerByIdMarker", (_, _, _) -> {
                })
                .extension(NHLPlayerList::promiseExtensionGetPlayerList)
                .then("findPlayerById", (_, _, promise) -> {
                    UtilTank01.Response response = promise.getRepositoryMapClass(UtilTank01.Response.class);
                    TelegramCommandContext context = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    Map<String, Object> player = NHLPlayerList.findById(
                            context.getUriParameters().get("idPlayer"),
                            response.getData()
                    );
                    if (player == null || player.isEmpty()) {
                        context.getTelegramBot().send(UtilTelegram.editMessage(context.getMsg(), "Not found player"));
                        promise.skipAllStep();
                        return;
                    }
                    String playerInfo = player.get("longName").toString() + " (" + player.get("team").toString() + ")";
                    context.getTelegramBot().send(UtilTelegram.editMessage(context.getMsg(), playerInfo));
                    context.getUriParameters().put("infoPlayer", playerInfo);
                    context.getUriParameters().put("idTeam", player.get("teamID").toString());
                })
                .extension(extendPromise -> UtilTank01.cacheRequest(
                        extendPromise,
                        promise -> {
                            TelegramCommandContext context = promise.getRepositoryMapClass(TelegramCommandContext.class);
                            return NHLTeamSchedule.getUri(
                                    context.getUriParameters().get("idTeam"),
                                    String.valueOf(Calendar.getInstance().get(Calendar.YEAR))
                            );
                        }
                ))
                .then("mergeScheduledGames", (_, _, promise) -> {
                    UtilTank01.Response response = promise.getRepositoryMapClass(UtilTank01.Response.class);
                    TelegramCommandContext context = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> game = (List<Map<String, Object>>) context.getAnyData().computeIfAbsent(
                            "findGames", _ -> new ArrayList<String>()
                    );
                    game.addAll(NHLTeamSchedule.findGame(response.getData()));
                })
                .extension(extendPromise -> UtilTank01.cacheRequest(
                        extendPromise,
                        promise -> {
                            TelegramCommandContext context = promise.getRepositoryMapClass(TelegramCommandContext.class);
                            return NHLTeamSchedule.getUri(
                                    context.getUriParameters().get("idTeam"),
                                    String.valueOf(Calendar.getInstance().get(Calendar.YEAR) + 1)
                            );
                        }
                ))
                .then("mergeScheduledGames", (_, _, promise) -> {
                    UtilTank01.Response response = promise.getRepositoryMapClass(UtilTank01.Response.class);
                    TelegramCommandContext context = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> game = (List<Map<String, Object>>) context.getAnyData().computeIfAbsent(
                            "findGames", _ -> new ArrayList<String>()
                    );
                    game.addAll(NHLTeamSchedule.findGame(response.getData()));
                })
                .thenWithResource("insertSchedule", JdbcResource.class, (_, _, promise, jdbcResource) -> {
                    TelegramCommandContext context = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> game = (List<Map<String, Object>>) context.getAnyData().get("findGames");
                    List<Map<String, Object>> sortGameByTime = NHLTeamSchedule.getSortGameByTime(game);
                    if (sortGameByTime.isEmpty()) {
                        context.getTelegramBot().send(UtilTelegram.editMessage(context.getMsg(), "Not found games"));
                        return;
                    }
                    Map<String, Object> map = sortGameByTime.getFirst();
                    jdbcResource.execute(new JdbcRequest(JTScheduler.INSERT)
                            .addArg("id_chat", UtilTelegram.getIdChat(context.getMsg()))
                            .addArg("id_player", context.getUriParameters().get("idPlayer"))
                            .addArg("id_team", context.getUriParameters().get("idTeam"))
                            .addArg("id_game", map.get("gameID"))
                            .addArg("time_game_start", new BigDecimal(
                                    map.get("gameTime_epoch").toString()
                            ).longValue() * 1000)
                            .addArg("about",
                                    "["
                                            + UtilDate.format(map.get("gameDate").toString(), "yyyyMMdd", "dd/MM/yyyy")
                                            + map.get("gameTime")
                                            + "] "
                                            + context.getUriParameters().get("infoPlayer")
                                            + "; "
                                            + map.get("homeTeam")
                                            + " vs "
                                            + map.get("awayTeam")
                            )
                            .setDebug(false)
                    );
                    String infoGame = "A subscription has been created for goals by player "
                            + context.getUriParameters().get("infoPlayer")
                            + " in the game between the "
                            + map.get("homeTeam")
                            + " and "
                            + map.get("awayTeam")
                            + ", scheduled for " +
                            UtilDate.format(map.get("gameDate").toString(), "yyyyMMdd", "dd/MM/yyyy")
                            + " at " + map.get("gameTime")
                            + " (UTC" + map.get("zone") + ")"
                            + ".";
                    context.getTelegramBot().send(UtilTelegram.editMessage(context.getMsg(), infoGame));
                });
    }

}
