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
import ru.jamsys.tank.data.NHLPlayerList;
import ru.jamsys.tank.data.NHLTeamSchedule;
import ru.jamsys.telegram.TelegramBotHandler;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class SubscribeToPlayer implements TelegramContext {

    @Getter
    @Setter
    @Accessors(chain = true)
    private long idChat;

    private final TelegramBotHandler handler = App.get(TelegramBotComponent.class).getHandler();

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

    private void findScheduledGames(Update msg) {
        App.get(ServicePromise.class).get(getClass(), 5_000L)
                .extension(promise -> promise.setRepositoryMapClass(SubscribeToPlayer.class, this))
                .extension(promise -> promise.setRepositoryMapClass(Update.class, msg))
                .extension(NHLPlayerList::promiseExtensionGetPlayerList)
                .then("findPlayerById", (_, _, promise) -> {
                    UtilTank01.Context context = promise.getRepositoryMapClass(UtilTank01.Context.class);
                    SubscribeToPlayer self = promise.getRepositoryMapClass(SubscribeToPlayer.class);
                    Update origMessage = promise.getRepositoryMapClass(Update.class);
                    Map<String, Object> player = NHLPlayerList.findById(self.getPlayer(), context.getData());
                    if (player == null || player.isEmpty()) {
                        self.send(UtilTelegram.editMessage(origMessage, "Not found"));
                        promise.skipAllStep();
                        return;
                    }
                    self.send(UtilTelegram.editMessage(
                            origMessage,
                            player.get("longName").toString() + " (" + player.get("team").toString() + ")")
                    );
                    promise.setRepositoryMap("teamId", player.get("teamID"));
                    promise.setRepositoryMap("season", "2025");
                })
                .extension(NHLTeamSchedule::promiseExtensionGetTeamSchedule)
                .then("findScheduledGames", (_, _, promise) -> {
                    UtilTank01.Context context = promise.getRepositoryMapClass(UtilTank01.Context.class);
                    SubscribeToPlayer self = promise.getRepositoryMapClass(SubscribeToPlayer.class);
                    List<Map<String, Object>> game = NHLTeamSchedule.findGame(context.getData());
                    self.send("Scheduled: " + game.size() + " games", null);
                })
                .run();
    }

    private void findPlayerByName() {
        App.get(ServicePromise.class).get(getClass(), 5_000L)
                .extension(promise -> promise.setRepositoryMapClass(SubscribeToPlayer.class, this))
                .extension(NHLPlayerList::promiseExtensionGetPlayerList)
                .then("findPlayerByName", (_, _, promise) -> {
                    UtilTank01.Context context = promise.getRepositoryMapClass(UtilTank01.Context.class);
                    SubscribeToPlayer self = promise.getRepositoryMapClass(SubscribeToPlayer.class);
                    List<Map<String, Object>> userList = NHLPlayerList.findByName(
                            self.getPlayer(),
                            context.getData()
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
