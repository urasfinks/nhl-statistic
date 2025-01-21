package ru.jamsys.telegram;

import lombok.Getter;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.jamsys.core.App;
import ru.jamsys.core.component.SecurityComponent;
import ru.jamsys.core.component.manager.item.RouteGeneratorRepository;
import ru.jamsys.core.component.manager.item.Session;
import ru.jamsys.core.extension.http.ServletRequestReader;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilTelegram;
import ru.jamsys.core.flat.util.telegram.Button;
import ru.jamsys.core.handler.promise.RemoveSubscriberOvi;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;

import java.io.InputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class TelegramBotLibSender extends TelegramLongPollingBot implements TelegramSender {

    private final RouteGeneratorRepository routerRepository;
    private final Map<Long, String> stepHandler;

    private static final TelegramBotsApi api;

    static {
        try {
            api = new TelegramBotsApi(DefaultBotSession.class);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    @Getter
    private final String botUsername;

    public static TelegramBotLibSender getInstance(
            BotProperty botProperty,
            RouteGeneratorRepository routerRepository
    ) throws TelegramApiException {
        Util.logConsole(
                TelegramBotLibSender.class,
                "Init bot: "
                        + botProperty.getName()
                        + "; SecurityAlias: "
                        + botProperty.getSecurityAlias()
        );
        return new TelegramBotLibSender(
                botProperty.getName(),
                new String(App.get(SecurityComponent.class).get(botProperty.getSecurityAlias())),
                routerRepository
        );
    }

    public TelegramBotLibSender(String botUsername, String botToken, RouteGeneratorRepository routerRepository) throws TelegramApiException {
        super(botToken);
        this.botUsername = botUsername;
        this.routerRepository = routerRepository;
        stepHandler = new Session<>("TelegramContext_" + botUsername, Long.class, 60_000L);
        api.registerBot(this);
    }

    @Override
    public void onUpdateReceived(Update msg) {
        if (msg == null) {
            return;
        }
        if (msg.hasCallbackQuery()) {
            send(
                    UtilTelegram.answerCallbackQuery(msg, ""),
                    UtilTelegram.getIdChat(msg)
            );
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
            if (idChat < 0) {
                send(UtilTelegram.message(
                        idChat,
                        "Группы не поддерживаются",
                        null
                ), idChat);
                return;
            }
            if (UtilTelegram.isBot(msg)) {
                send(UtilTelegram.message(
                        idChat,
                        "Боты не поддерживаются",
                        null
                ), idChat);
                return;
            }
            if (data.startsWith("/start ")) {
                data = "/start/?playload=" + data.substring(7);
            }
            PromiseGenerator match = routerRepository.match(data);
            if (match == null) {
                send(UtilTelegram.message(
                        idChat,
                        "Команда " + UtilTelegram.getData(msg) + " не поддерживается",
                        null
                ), idChat);
                return;
            }
            Promise promise = match.generate();
            if (promise == null) {
                App.error(new RuntimeException("Promise is null"));
                return;
            }
            if (!promise.isSetErrorHandler()) {
                promise.onError((_, _, p) -> Util.logConsole(getClass(), p.getLogString()));
            }

            promise.setRepositoryMapClass(TelegramCommandContext.class, new TelegramCommandContext()
                    .setUserInfo(UtilTelegram.getUserInfo(msg))
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

    public UtilTelegram.Result send(long idChat, String data, List<Button> buttons) {
        return send(UtilTelegram.message(idChat, data, buttons), idChat);
    }

    @SuppressWarnings("all")
    public <T extends Serializable, Method extends BotApiMethod<T>> UtilTelegram.Result send(Method method, Long idChat) {
        if (idChat == null) {
            return new UtilTelegram.Result()
                    .setException(UtilTelegram.ResultException.ID_CHAT_EMPTY)
                    .setCause("idChat is null");
        }
        UtilTelegram.Result sandbox = UtilTelegram.sandbox(result -> {
            result.setResponse(execute(method));
        });
        if (UtilTelegram.ResultException.BLOCK.equals(sandbox.getException())) {
            new RemoveSubscriberOvi(idChat).generate().run();
        }
        return sandbox;
    }

    public UtilTelegram.Result sendImage(long idChat, InputStream is, String fileName, String description) {
        UtilTelegram.Result sandbox = UtilTelegram.sandbox(result -> {
            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setChatId(idChat);
            sendPhoto.setPhoto(new InputFile(is, fileName));
            if (description != null) {
                sendPhoto.setCaption(description);
            }
            result.setResponse(execute(sendPhoto));
        });
        if (UtilTelegram.ResultException.BLOCK.equals(sandbox.getException())) {
            new RemoveSubscriberOvi(idChat).generate().run();
        }
        return sandbox;
    }

    @Override
    public void setMyCommands(SetMyCommands setMyCommands) throws TelegramApiException {
        execute(setMyCommands);
    }


}
