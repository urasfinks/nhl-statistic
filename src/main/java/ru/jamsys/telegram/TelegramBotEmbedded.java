package ru.jamsys.telegram;

import lombok.Getter;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.File;
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
import ru.jamsys.core.flat.util.UtilFile;
import ru.jamsys.core.flat.util.UtilTelegramMessage;
import ru.jamsys.core.flat.util.UtilTelegramResponse;
import ru.jamsys.core.flat.util.telegram.Button;
import ru.jamsys.core.handler.promise.RemoveSubscriberOvi;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.util.List;
import java.util.Map;

public class TelegramBotEmbedded extends TelegramLongPollingBot implements TelegramSender {

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


    private final BotProperty botProperty;

    public static TelegramBotEmbedded getInstance(
            BotProperty botProperty,
            RouteGeneratorRepository routerRepository
    ) throws TelegramApiException {
        Util.logConsole(
                TelegramBotEmbedded.class,
                "Init bot: "
                        + botProperty.getName()
                        + "; SecurityAlias: "
                        + botProperty.getSecurityAlias()
        );
        return new TelegramBotEmbedded(
                botProperty,
                routerRepository
        );
    }


    public TelegramBotEmbedded(BotProperty botProperty, RouteGeneratorRepository routerRepository) throws TelegramApiException {
        super(new String(App.get(SecurityComponent.class).get(botProperty.getSecurityAlias())));
        this.botProperty = botProperty;
        this.routerRepository = routerRepository;
        stepHandler = new Session<>("TelegramContext_" + botProperty.getName(), 600_000L);
        api.registerBot(this);
    }

    @Override
    public void onUpdateReceived(Update msg) {
        if (msg == null) {
            return;
        }
        if (msg.hasCallbackQuery()) {
            send(
                    UtilTelegramMessage.answerCallbackQuery(msg, ""),
                    UtilTelegramMessage.getIdChat(msg)
            );
        }
        Long idChat = UtilTelegramMessage.getIdChat(msg);
        if (idChat == null) {
            return;
        }
        String data = UtilTelegramMessage.getData(msg);
        if (data == null) {
            // Если надо допустим принять картинку или видео
            // Сообщения же реально может не быть
            data = "";
        }
        String remove = stepHandler.remove(idChat);

        if (remove == null && notCommandPrefix != null) {
            remove = notCommandPrefix;
        }

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
        System.out.println(data);
        if (data.startsWith("/")) {
            if (idChat < 0) {
                send(UtilTelegramMessage.message(
                        idChat,
                        "Группы не поддерживаются",
                        null
                ), idChat);
                return;
            }
            if (UtilTelegramMessage.isBot(msg)) {
                send(UtilTelegramMessage.message(
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
                send(UtilTelegramMessage.message(
                        idChat,
                        "Команда " + UtilTelegramMessage.getData(msg) + " не поддерживается",
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
                    .setUserInfo(UtilTelegramMessage.getUserInfo(msg))
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

    public UtilTelegramResponse.Result send(long idChat, String data, List<Button> buttons) {
        return send(UtilTelegramMessage.message(idChat, data, buttons), idChat);
    }

    @SuppressWarnings("all")
    public <T extends Serializable, Method extends BotApiMethod<T>> UtilTelegramResponse.Result send(Method method, Long idChat) {
        if (idChat == null) {
            return new UtilTelegramResponse.Result()
                    .setException(UtilTelegramResponse.ResultException.ID_CHAT_EMPTY)
                    .setCause("idChat is null");
        }
        long startTime = System.currentTimeMillis();
        UtilTelegramResponse.Result sandbox = UtilTelegramResponse.sandbox(result -> {
            result.setResponse(execute(method));
        });
        if (UtilTelegramResponse.ResultException.REVOKE.equals(sandbox.getException())) {
            new RemoveSubscriberOvi(idChat).generate().run();
        }
        sandbox.setRequestTiming(System.currentTimeMillis() - startTime);
        return sandbox;
    }

    public UtilTelegramResponse.Result sendImage(long idChat, InputStream is, String fileName, String description) {
        UtilTelegramResponse.Result sandbox = UtilTelegramResponse.sandbox(result -> {
            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setChatId(idChat);
            sendPhoto.setPhoto(new InputFile(is, fileName));
            if (description != null) {
                sendPhoto.setCaption(description);
            }
            result.setResponse(execute(sendPhoto));
        });
        if (UtilTelegramResponse.ResultException.REVOKE.equals(sandbox.getException())) {
            new RemoveSubscriberOvi(idChat).generate().run();
        }
        return sandbox;
    }

    @Override
    public void setMyCommands(SetMyCommands setMyCommands) throws TelegramApiException {
        execute(setMyCommands);
    }

    @Getter
    private String notCommandPrefix = null; //Если приход чистое сообщение от пользователя без команды и нет данных из шага


    @Override
    public TelegramSender setNotCommandPrefix(String notCommandPrefix) {
        this.notCommandPrefix = notCommandPrefix;
        return this;
    }

    @Override
    public UtilTelegramResponse.Result sendImage(long idChat, String idFile, String description) {
        throw new RuntimeException("unsupported");
    }

    @Override
    public UtilTelegramResponse.Result sendVideo(long idChat, String idFile, String description) {
        throw new RuntimeException("unsupported");
    }

    @SuppressWarnings("unused")
    public String downloadFileCustom(String fileId) throws TelegramApiException, IOException {
        GetFile getFile = new GetFile();
        getFile.setFileId(fileId);
        File file = execute(getFile);
        //{
        //  "file_id" : "AgACAgIAAxkBAAIS6Wed-TkCuCanrYFe3p4nfVKFrJHdAAIg7zEbui3wSBrhvpLj7DInAQADAgADeQADNgQ",
        //  "file_unique_id" : "AQADIO8xG7ot8Eh-",
        //  "file_size" : 34481,
        //  "file_path" : "photos/file_0.jpg"
        //}
        String fileUrl = file.getFileUrl(new String(App.get(SecurityComponent.class).get(botProperty.getSecurityAlias())));
        try (InputStream is = URI.create(fileUrl).toURL().openStream()) {
            String fileName = "file_" + java.util.UUID.randomUUID() + "." + UtilFile.getExtension(file.getFilePath());
            UtilFile.writeBytes(fileName, is);
            return fileName;
        }
    }

    @Override
    public String getBotUsername() {
        return botProperty.getName();
    }

}
