package ru.jamsys.telegram;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.jamsys.core.App;
import ru.jamsys.core.component.TelegramBotComponent;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.flat.util.UtilTelegram;
import ru.jamsys.telegram.command.TelegramContext;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TelegramBotHandler extends TelegramLongPollingBot {

    public TelegramBotHandler(String botToken) throws TelegramApiException {
        super(botToken);
        List<BotCommand> list = new ArrayList<>();
        list.add(new BotCommand("/subscribe_to_player", "Follow a player to stay updated"));
        list.add(new BotCommand("/my_subscriptions", "See the list of players you're following"));
        list.add(new BotCommand("/remove_subscription", "Unfollow a player from your list"));
        execute(new SetMyCommands(list, new BotCommandScopeDefault(), null));
    }

    @Override
    public void onUpdateReceived(Update msg) {
        try {
            System.out.println(UtilJson.toStringPretty(msg, "{}"));
            if (msg != null) {
                if (msg.hasCallbackQuery()) {
                    execute(UtilTelegram.answerCallbackQuery(msg, ""));
                }
                handler(msg);
            }
        } catch (Throwable e) {
            App.error(e);
        }
    }

    private void handler(Update msg) throws Throwable {
        Long idChat = UtilTelegram.getIdChat(msg);
        if (idChat == null) {
            return;
        }
        Map<Long, TelegramContext> map = App.get(TelegramBotComponent.class).getMap();

        // Если что-то начинается на "/" - это всё конец всем предыдущим
        if (msg.hasMessage() && msg.getMessage().getText().startsWith("/")) {
            if (map.containsKey(idChat)) {
                map.get(idChat).finish();
            }
            TelegramCommand telegramCommand = TelegramCommand.valueOfCommand(msg.getMessage().getText());
            if (telegramCommand != null) {
                TelegramContext telegramContext = telegramCommand.getTelegramContext(idChat);
                map.put(idChat, telegramContext);
                telegramContext.start();
            } else {
                send(UtilTelegram.message(
                        idChat,
                        "Command " + msg.getMessage().getText() + " not support",
                        null
                ));
            }
        } else if (map.containsKey(idChat)) {
            map.get(idChat).onNext(msg);
        } else if (msg.hasCallbackQuery()) {
            send(UtilTelegram.message(idChat, "Operation canceled, please start over", null));
        }
    }

    public <T extends Serializable, Method extends BotApiMethod<T>> T send(Method method) {
        try {
            execute(method);
        } catch (Throwable th) {
            App.error(th);
        }
        return null;
    }

    @Override
    public String getBotUsername() {
        return "ovi_goals_bot";
    }

}
