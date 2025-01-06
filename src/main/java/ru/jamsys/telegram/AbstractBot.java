package ru.jamsys.telegram;

import lombok.Getter;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
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

public abstract class AbstractBot extends TelegramLongPollingBot {

    private final RouteGeneratorRepository routerRepository;
    private final Map<Long, String> stepHandler;

    @Getter
    private final String botUsername;

    public AbstractBot(String botUsername, String botToken, RouteGeneratorRepository routerRepository) {
        super(botToken);
        this.botUsername = botUsername;
        this.routerRepository = routerRepository;
        stepHandler = new Session<>("TelegramContext_" + botUsername, Long.class, 60_000L);
    }

    @Override
    public void onUpdateReceived(Update msg) {
        if (msg == null) {
            return;
        }
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
            if (idChat < 0) {
                send(UtilTelegram.message(
                        idChat,
                        "Группы не поддерживаются",
                        null
                ));
                return;
            }
            if (UtilTelegram.isBot(msg)) {
                send(UtilTelegram.message(
                        idChat,
                        "Боты не поддерживаются",
                        null
                ));
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

    public void send(long idChat, String data, List<Button> buttons) {
        send(UtilTelegram.message(idChat, data, buttons));
    }

    @SuppressWarnings("all")
    public <T extends Serializable, Method extends BotApiMethod<T>> T send(Method method) {
        try {
            execute(method);
        } catch (Throwable th) {
            App.error(th);
        }
        return null;
    }

    @SuppressWarnings("unused")
    public static List<String> splitMessageSmart(String message, int maxLength) {
        List<String> parts = new ArrayList<>();

        while (message.length() > maxLength) {
            // Попробуем найти перенос строки или конец предложения
            int splitIndex = findSplitIndex(message, maxLength);

            // Добавляем часть текста в список
            parts.add(message.substring(0, splitIndex).trim());
            // Обрезаем обработанную часть
            message = message.substring(splitIndex).trim();
        }

        // Добавляем оставшийся текст
        if (!message.isEmpty()) {
            parts.add(message);
        }

        return parts;
    }

    public static int findSplitIndex(String message, int maxLength) {
        // Ищем последний перенос строки до maxLength
        int newlineIndex = message.lastIndexOf('\n', maxLength);
        if (newlineIndex != -1) {
            return newlineIndex + 1; // Включаем перенос строки
        }

        // Ищем конец предложения (точка, восклицательный знак, вопросительный знак)
        int sentenceEndIndex = Math.max(
                Math.max(message.lastIndexOf('.', maxLength), message.lastIndexOf('!', maxLength)),
                message.lastIndexOf('?', maxLength)
        );
        if (sentenceEndIndex != -1) {
            return sentenceEndIndex + 1; // Включаем знак конца предложения
        }

        // Если ничего не найдено, разрезаем по maxLength
        return maxLength;
    }

}
