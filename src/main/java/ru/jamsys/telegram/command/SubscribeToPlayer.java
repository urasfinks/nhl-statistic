package ru.jamsys.telegram.command;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.TelegramBotComponent;
import ru.jamsys.core.flat.util.UtilTelegram;
import ru.jamsys.core.flat.util.tank.UtilTank01;
import ru.jamsys.core.flat.util.telegram.Button;
import ru.jamsys.core.jt.JTScheduler;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;
import ru.jamsys.tank.data.NHLPlayerList;
import ru.jamsys.tank.data.NHLTeamSchedule;
import ru.jamsys.telegram.TelegramBot;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;


public class SubscribeToPlayer implements TelegramContext {

    @Getter
    @Setter
    @Accessors(chain = true)
    private long idChat;

    private final TelegramBot handler = App.get(TelegramBotComponent.class).getHandler();

    @Getter
    private String player = null;

    @Override
    public void onNext(Update msg) {
        try {
            if (msg.hasMessage() && player == null) {
                player = msg.getMessage().getText();
                findPlayerByName();
            } else if (msg.hasCallbackQuery()) {
                player = UtilTelegram.getData(msg);
                if (player == null) {
                    return;
                }
                if (player.equals("cancel")) {
                    handler.send(UtilTelegram.removeMessage(msg));
                    return;
                }
                findScheduledGames(msg);
            }
        } catch (Throwable th) {
            App.error(th);
        }
    }

    @Getter
    @Setter
    @Accessors(chain = true)
    public static class Context {
        private SubscribeToPlayer self;
        private Update msg;
        private List<Map<String, Object>> findGames = new ArrayList<>();
        private JdbcRequest insert = new JdbcRequest(JTScheduler.INSERT);
        private String teamId;
        private int season;
    }

    private void findScheduledGames(Update msg) {
        Promise p = App.get(ServicePromise.class).get(getClass(), 5_000L);
        p.setRepositoryMapClass(Context.class, new Context()
                .setSelf(this)
                .setMsg(msg)
        );
        p
                .extension(NHLPlayerList::promiseExtensionGetPlayerList)
                .then("findPlayerById", (_, _, promise) -> {
                    UtilTank01.Response response = promise.getRepositoryMapClass(UtilTank01.Response.class);
                    Context context = promise.getRepositoryMapClass(Context.class);
                    Map<String, Object> player = NHLPlayerList.findById(
                            context.getSelf().getPlayer(),
                            response.getData()
                    );
                    if (player == null || player.isEmpty()) {
                        context.getSelf().send(UtilTelegram.editMessage(context.getMsg(), "Not found player"));
                        promise.skipAllStep();
                        return;
                    }
                    context.getSelf().send(UtilTelegram.editMessage(
                            context.getMsg(),
                            player.get("longName").toString() + " (" + player.get("team").toString() + ")")
                    );
                    context.setTeamId(player.get("teamID").toString());
                    context.setSeason(Calendar.getInstance().get(Calendar.YEAR));
                })
                .extension(extendPromise -> UtilTank01.cacheRequest(
                        extendPromise,
                        promise -> {
                            Context context = promise.getRepositoryMapClass(Context.class);
                            return NHLTeamSchedule.getUri(context.getTeamId(), context.getSeason());
                        }
                ))
                .then("mergeScheduledGames", (_, _, promise) -> {
                    UtilTank01.Response response = promise.getRepositoryMapClass(UtilTank01.Response.class);
                    Context context = promise.getRepositoryMapClass(Context.class);
                    context.getFindGames().addAll(NHLTeamSchedule.findGame(response.getData()));
                })
                .extension(extendPromise -> UtilTank01.cacheRequest(
                        extendPromise,
                        promise -> {
                            Context context = promise.getRepositoryMapClass(Context.class);
                            return NHLTeamSchedule.getUri(context.getTeamId(), context.getSeason() + 1);
                        }
                ))
                .then("mergeScheduledGames", (_, _, promise) -> {
                    UtilTank01.Response response = promise.getRepositoryMapClass(UtilTank01.Response.class);
                    Context context = promise.getRepositoryMapClass(Context.class);
                    context.getFindGames().addAll(NHLTeamSchedule.findGame(response.getData()));
                })
                .thenWithResource("insertSchedule", JdbcResource.class, (_, _, promise, jdbcResource) -> {
                    Context context = promise.getRepositoryMapClass(Context.class);
                    List<Map<String, Object>> sortGameByTime = NHLTeamSchedule.getSortGameByTime(context.getFindGames());
                    if (sortGameByTime.isEmpty()) {
                        context.getSelf().send(UtilTelegram.editMessage(context.getMsg(), "Not found games"));
                        promise.skipAllStep();
                        return;
                    }
                    Map<String, Object> map = sortGameByTime.getFirst();
                    context.getInsert()
                            .addArg("id_chat", UtilTelegram.getIdChat(context.getMsg()))
                            .addArg("id_player", context.getSelf().getPlayer())
                            .addArg("id_team", context.getTeamId())
                            .addArg("id_game", map.get("gameID"))
                            .addArg("time_game_start", new BigDecimal(
                                    map.get("gameTime_epoch").toString()
                            ).longValue() * 1000)
                            .addArg("about", map.get("about"))
                            .setDebug(false);
                    jdbcResource.execute(context.getInsert());
                })
                .onError((_, _, promise) -> System.out.println(promise.getLogString()))
                .run();
    }

    private void findPlayerByName() {
        App.get(ServicePromise.class).get(getClass(), 5_000L)
                .extension(promise -> promise.setRepositoryMapClass(SubscribeToPlayer.class, this))
                .extension(NHLPlayerList::promiseExtensionGetPlayerList)
                .then("findPlayerByName", (_, _, promise) -> {
                    UtilTank01.Response response = promise.getRepositoryMapClass(UtilTank01.Response.class);
                    SubscribeToPlayer self = promise.getRepositoryMapClass(SubscribeToPlayer.class);
                    List<Map<String, Object>> userList = NHLPlayerList.findByName(
                            self.getPlayer(),
                            response.getData()
                    );
                    if (userList.isEmpty()) {
                        self.send("Player's not found", null);
                        return;
                    }
                    int counter = 0;
                    List<Button> buttons = new ArrayList<>();
                    for (Map<String, Object> player : userList) {
                        buttons.add(new Button(
                                player.get("longName").toString() + " (" + player.get("team").toString() + ")",
                                player.get("playerID").toString())
                        );
                        counter++;
                        if (counter > 2) {
                            break;
                        }
                    }
                    buttons.add(new Button("Cancel", "cancel"));
                    self.send("Choose Player:", buttons);
                })
                .run();
    }

    public void send(String data, List<Button> buttons) {
        handler.send(UtilTelegram.message(idChat, data, buttons));
    }

    public <T extends Serializable, Method extends BotApiMethod<T>> T send(Method method) {
        return handler.send(method);
    }

    @Override
    public void start() {
        send("Enter the player's name", null);
    }

    @Override
    public void finish() {

    }

}
