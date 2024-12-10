package ru.jamsys.telegram;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.item.RouteGeneratorRepository;
import ru.jamsys.core.component.manager.item.Session;
import ru.jamsys.core.extension.http.ServletRequestReader;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilTelegram;
import ru.jamsys.core.flat.util.telegram.Button;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TelegramBot extends TelegramLongPollingBot {

    private final RouteGeneratorRepository routerRepository;
    private final Map<Long, String> stepHandler;

    public TelegramBot(String botToken, RouteGeneratorRepository routerRepository) throws TelegramApiException {
        super(botToken);
        this.routerRepository = routerRepository;
        List<BotCommand> list = new ArrayList<>();
        list.add(new BotCommand("/subscribe_to_player", "Follow a player to stay updated"));
        list.add(new BotCommand("/my_subscriptions", "See the list of players you're following"));
        list.add(new BotCommand("/remove_subscription", "Unfollow a player from your list"));
        execute(new SetMyCommands(list, new BotCommandScopeDefault(), null));

        stepHandler = new Session<>("TelegramContext", Long.class, 60_000L);
    }

    @Override
    public void onUpdateReceived(Update msg) {
        if (msg == null) {
            return;
        }
        //System.out.println(UtilJson.toStringPretty(msg, "{}"));
        if (msg.hasCallbackQuery()) {
            send(UtilTelegram.answerCallbackQuery(msg, ""));
        }
        Long idChat = UtilTelegram.getIdChat(msg);
        if (idChat == null) {
            return;
        }
        String data = UtilTelegram.getData(msg);
        if (data == null) {
            return;
        }
        String remove = stepHandler.remove(idChat);

        // Тут 2 варианта:
        // 1) Приходит чистое сообщение от пользователя
        // 2) Приходит ButtonCallbackData - подразумевает, что имеет полный путь /command/?args=...
        // Не должно быть чистого сообщения от пользователя содержащего контекст и начало с /
        if (remove != null && msg.hasMessage() && data.startsWith("/")) {
            remove = null;
        }
        if (remove != null) {
            try {
                data = remove + Util.urlEncode(data);
            } catch (Exception e) {
                App.error(e);
            }
        }
        if (data.startsWith("/")) {
            PromiseGenerator match = routerRepository.match(data);
            if (match == null) {
                send(UtilTelegram.message(
                        idChat,
                        "Command " + msg.getMessage().getText() + " not support",
                        null
                ));
                return;
            }
            Promise promise = match.generate();
            if (promise == null) {
                App.error(new RuntimeException("Promise is null"));
                return;
            }
            if (!promise.isSetErrorHandler()) {
                promise.onError((_, _, p) -> System.out.println(p.getLogString()));
            }
            promise.setRepositoryMapClass(TelegramCommandContext.class, new TelegramCommandContext()
                    .setIdChat(idChat)
                    .setMsg(msg)
                    .setStepHandler(stepHandler)
                    .setUriPath(ServletRequestReader.getPath(data))
                    .setUriParametersListValue(ServletRequestReader.parseUriParameters(data))
                    .setUriParameters(ServletRequestReader.parseUriParameters(data, listString -> {
                        try {
                            return Util.urlDecode(listString.getFirst());
                        } catch (Exception e) {
                            App.error(e);
                        }
                        return listString.getFirst();
                    }))
                    .setTelegramBot(this)
            );
            promise.run();
        }
    }

    public void send(long idChat, String data, List<Button> buttons) {
        send(UtilTelegram.message(idChat, data, buttons));
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
