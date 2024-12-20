package ru.jamsys.core.handler.telegram;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.http.ServletResponseWriter;
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
import java.util.List;
import java.util.Map;

@Setter
@Getter
@Component
@RequestMapping({"/subscribe_to_player/**", "/stp/**"})
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
                    if (NHLTeamSchedule.getCurrentSeasonIfRunOrNext() == null) {
                        context.getTelegramBot().send(context.getIdChat(), "The regular season has not started yet. The subscription is available from October to April.", null);
                        promise.skipAllStep("Not found run season");
                        return;
                    }
                    if (!context.getUriParameters().containsKey("namePlayer")) {
                        context.getStepHandler().put(context.getIdChat(), context.getUriPath() + "/?namePlayer=");
                        context.getTelegramBot().send(
                                context.getIdChat(),
                                "Enter the player's name:",
                                null
                        );
                        promise.skipAllStep("wait player name");
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
                                        "/stp/",
                                        new HashMapBuilder<>(context.getUriParameters())
                                                .append("idPlayer", player.get("playerID").toString())
                                )
                        ));
                        counter++;
                        if (counter > 9) {
                            break;
                        }
                    }
                    context.getTelegramBot().send(context.getIdChat(), "Choose Player:", buttons);
                    promise.skipAllStep("wait id_player");
                })
                .then("findPlayerByIdMarker", (_, _, _) -> {
                })
                .thenWithResource("checkAlready", JdbcResource.class, (_, _, promise, jdbcResource) -> {
                    TelegramCommandContext context = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    List<Map<String, Object>> execute = jdbcResource.execute(new JdbcRequest(JTScheduler.SELECT_MY_SUBSCRIBED_GAMES)
                            .addArg("id_chat", context.getIdChat())
                            .addArg("id_player", context.getUriParameters().get("idPlayer"))
                            .setDebug(false)
                    );
                    if (!execute.isEmpty()) {
                        context.getTelegramBot().send(UtilTelegram.editMessage(context.getMsg(), "The subscription already exists."));
                        promise.skipAllStep("The subscription already exists");
                    }
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
                        promise.skipAllStep("Not found player");
                        return;
                    }
                    String playerInfo = NHLPlayerList.getPlayerName(player);
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
                                    NHLTeamSchedule.getCurrentSeasonIfRunOrNext() + ""
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
                    game.addAll(NHLTeamSchedule.parseGame(response.getData()));
                })
                .thenWithResource("insertSchedule", JdbcResource.class, (_, _, promise, jdbcResource) -> {
                    TelegramCommandContext context = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> game = (List<Map<String, Object>>) context.getAnyData().get("findGames");
                    List<Map<String, Object>> sortGameByTime = NHLTeamSchedule.getGameSortAndFilterByTime(game);
                    if (sortGameByTime.isEmpty()) {
                        context.getTelegramBot().send(UtilTelegram.editMessage(context.getMsg(), "Not found games"));
                        return;
                    }
                    JdbcRequest jdbcRequest = new JdbcRequest(JTScheduler.INSERT);
                    sortGameByTime.forEach(map -> jdbcRequest
                            .addArg("id_chat", UtilTelegram.getIdChat(context.getMsg()))
                            .addArg("id_player", context.getUriParameters().get("idPlayer"))
                            .addArg("id_team", context.getUriParameters().get("idTeam"))
                            .addArg("id_game", map.get("gameID"))
                            .addArg("time_game_start", new BigDecimal(
                                    map.get("gameTime_epoch").toString()
                            ).longValue() * 1000)
                            .addArg("game_about", NHLTeamSchedule.getGameAbout(map))
                            .addArg("player_about", context.getUriParameters().get("infoPlayer"))
                            .addArg("test", UtilJson.toStringPretty(map, "{}"))
                            .nextBatch());

                    jdbcResource.execute(jdbcRequest);

                    context.getTelegramBot().send(UtilTelegram.editMessage(context.getMsg(), String.format(
                            "A subscription for %d games featuring %s has been created." +
                                    " The first game is on %s, and the last game is on %s." +
                                    " See more information in the subscriptions.",

                            sortGameByTime.size(),
                            context.getUriParameters().get("infoPlayer"),
                            NHLTeamSchedule.getGameTimeFormat(sortGameByTime.getFirst()),
                            NHLTeamSchedule.getGameTimeFormat(sortGameByTime.getLast())
                    )));
                });
    }

}
