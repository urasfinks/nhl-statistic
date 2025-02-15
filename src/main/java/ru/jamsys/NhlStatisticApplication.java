package ru.jamsys;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import ru.jamsys.core.App;
import ru.jamsys.core.component.TelegramBotManager;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.flat.util.UtilTelegramMessage;
import ru.jamsys.core.flat.util.UtilTelegramResponse;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.statistic.AvgMetric;
import ru.jamsys.telegram.TelegramCommandContext;
import ru.jamsys.telegram.TelegramNotification;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

@SpringBootApplication
public class NhlStatisticApplication {

    public static boolean startTelegramListener = true; // Запускать чтение сообщений от бота

    public static void main(String[] args) {
        System.setProperty("file.encoding", "UTF-8");
        App.springSource = NhlStatisticApplication.class;
        App.main(args);
    }

    @SuppressWarnings("unused")
    public static void testSend() {
        TelegramBotManager telegramBotManager = App.get(TelegramBotManager.class);
        TelegramNotification telegramNotification = new TelegramNotification(
                290029195L,
                telegramBotManager.getOviBotProperty().getName(),
                "Привет",
                null,
                null
        );
        UtilTelegramResponse.Result send = telegramBotManager.send(telegramNotification, TelegramBotManager.TypeSender.HTTP);
        Util.logConsoleJson(NhlStatisticApplication.class, send);
    }

    public static void addOnError(Promise sourcePromise) {
        sourcePromise.onError((_, _, promise) -> {
            try {
                // Сначала надо себе отправить лог поломки, так как БД может быть не доступно
                TelegramCommandContext context = promise.getRepositoryMapClass(TelegramCommandContext.class);
                App.get(TelegramBotManager.class).send(new TelegramNotification(
                        290029195L,
                        App.get(TelegramBotManager.class).getCommonBotProperty().getName(),
                        "Сервис временно не работает. " + UtilJson.toStringPretty(context.getMsg(), "{}"),
                        null,
                        null
                ), TelegramBotManager.TypeSender.HTTP);
                // Когда что-то ломается, мы не можем стандартным механизмом отправлять сообщения через очередь в БД
                // возможно БД лежит или другие проблемы с ядром/коннектами/ошибками
                context.getTelegramBot().send(UtilTelegramMessage.message(
                        context.getIdChat(),
                        "Сервис временно не работает. Повторите попытку позже",
                        null
                ), context.getIdChat());
            } catch (Throwable th) {
                App.error(th);
            }
        });
    }

    @SuppressWarnings("unused")
    public static void loadTelegram() {
        TelegramBotManager telegramBotManager = App.get(TelegramBotManager.class);
        Queue<TelegramNotification> queue = new ConcurrentLinkedQueue<>();
        for (int i = 0; i < 30; i++) {
            queue.add(new TelegramNotification(
                    290029195L,
                    telegramBotManager.getOviBotProperty().getName(),
                    i + "",
                    null,
                    null
            ));
            queue.add(new TelegramNotification(
                    294097034L,
                    telegramBotManager.getOviBotProperty().getName(),
                    i + "",
                    null,
                    null
            ));
            queue.add(new TelegramNotification(
                    241022301L,
                    telegramBotManager.getOviBotProperty().getName(),
                    i + "",
                    null,
                    null
            ));
        }
        long start = System.currentTimeMillis();
        AvgMetric avg = new AvgMetric();
        AtomicBoolean a = new AtomicBoolean(false);
        AtomicBoolean isRun = new AtomicBoolean(true);
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                while (isRun.get()) {
                    TelegramNotification poll = queue.poll();
                    if (poll != null) {
                        UtilTelegramResponse.Result send = telegramBotManager.send(poll, TelegramBotManager.TypeSender.HTTP);
                        avg.add(send.getRequestTiming());
                    } else {
                        break;
                    }
                }
                System.out.println(System.currentTimeMillis() - start);
                if (a.compareAndSet(false, true)) {
                    Util.sleepMs(5000);
                    Util.logConsoleJson(NhlStatisticApplication.class, "Timing send: ", avg.flushInstance());
                }
            }).start();
        }
    }


}
