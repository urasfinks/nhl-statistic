package ru.jamsys.telegram.command;

import org.telegram.telegrambots.meta.api.objects.Update;
import ru.jamsys.core.App;
import ru.jamsys.core.component.TelegramBotComponent;
import ru.jamsys.tank.data.NHLPlayerList;
import ru.jamsys.telegram.Button;
import ru.jamsys.telegram.TelegramBotHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SubscribeToPlayer implements TelegramContext {

    private long idChat;

    TelegramBotHandler telegramBotHandler = App.get(TelegramBotComponent.class).getTelegramBotHandler();

    @Override
    public long getIdChat() {
        return idChat;
    }

    @Override
    public TelegramContext setIdChat(long idChat) {
        this.idChat = idChat;
        return this;
    }

    String player = null;

    @Override
    public void onNext(Update msg) {
        try {
            if (msg.hasMessage() && player == null) {
                List<Map<String, Object>> byName = NHLPlayerList.findByName(msg.getMessage().getText(), NHLPlayerList.getExample());
                if (byName.isEmpty()) {
                    notFoundPlayer();
                    return;
                }
                player = msg.getMessage().getText();
                int counter = 0;
                List<Button> buttons = new ArrayList<>();
                for (Map<String, Object> player : byName) {
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
                foundPlayer(buttons);
            } else if (msg.hasCallbackQuery()) {
                String data = telegramBotHandler.getData(msg);
                if (data.equals("cancel")) {
                    telegramBotHandler.removeMessage(msg);
                    return;
                }
                Map<String, Object> player = NHLPlayerList.findById(data, NHLPlayerList.getExample());
                if (player == null || player.isEmpty()) {
                    telegramBotHandler.changeMessage(msg, "Not found");
                    return;
                }
                telegramBotHandler.changeMessage(
                        msg,
                        player.get("longName").toString() + " (" + player.get("team").toString() + ")"
                );

            }
        } catch (Throwable th) {
            App.error(th);
        }
    }

    private void foundPlayer(List<Button> buttons) {
        try {
            telegramBotHandler.send(idChat, "Choose Player:", buttons);
        } catch (Throwable th) {
            App.error(th);
        }
    }

    private void notFoundPlayer() {
        try {
            telegramBotHandler.send(idChat, "Player's not found", null);
        } catch (Throwable th) {
            App.error(th);
        }
    }

    @Override
    public void start() {
        try {
            telegramBotHandler.send(idChat, "Enter the player's name", null);
        } catch (Throwable th) {
            App.error(th);
        }
    }

}
