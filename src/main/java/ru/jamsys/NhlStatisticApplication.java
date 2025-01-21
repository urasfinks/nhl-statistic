package ru.jamsys;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import ru.jamsys.core.App;
import ru.jamsys.core.component.TelegramBotManager;
import ru.jamsys.core.extension.builder.ArrayListBuilder;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.http.ServletResponseWriter;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilTelegram;
import ru.jamsys.core.flat.util.telegram.Button;
import ru.jamsys.core.statistic.AvgMetric;
import ru.jamsys.telegram.TelegramNotification;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

@SpringBootApplication
public class NhlStatisticApplication {

    public static boolean startTelegramListener = true; // Запускать чтение сообщений от бота

    public static void main(String[] args) {
        App.springSource = NhlStatisticApplication.class;
        App.main(args);
        TelegramNotification telegramNotification = new TelegramNotification(
                290029195L,
                App.get(TelegramBotManager.class).getOviBotProperty().getName(),
                """
                        Матч Pittsburgh Penguins (PIT) 🆚 Washington Capitals (WSH) начнется уже через 12 часов — 19 января в 03:00 (МСК).

                        Как думаешь, сможет ли Александр Овечкин забить сегодня?

                        """,
                new ArrayListBuilder<Button>()
                        .append(new Button(
                                "Да 🔥",
                                ServletResponseWriter.buildUrlQuery(
                                        "/poll_quest/",
                                        new HashMapBuilder<String, String>()
                                                .append("value", "true")

                                )
                        ))
                        .append(new Button(
                                "Нет ⛔",
                                ServletResponseWriter.buildUrlQuery(
                                        "/poll_quest/",
                                        new HashMapBuilder<String, String>()
                                                .append("value", "false")

                                )
                        ))
                ,
                null
        );
        App.get(TelegramBotManager.class).send(telegramNotification, TelegramBotManager.TypeSender.HTTP);

    }

    @SuppressWarnings("unused")
    public static void loadTelegram(){
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
        for (int i = 0; i < 1; i++) {
            new Thread(() -> {

                while (isRun.get()) {
                    TelegramNotification poll = queue.poll();
                    if (poll != null) {
                        UtilTelegram.Result send = telegramBotManager.send(poll, TelegramBotManager.TypeSender.HTTP);
                        avg.add(send.getTiming());
                    }else{
                        Util.sleepMs(1000);
                    }
                }
                if (a.compareAndSet(false, true)) {
                    Util.sleepMs(5000);
                    Util.logConsoleJson(NhlStatisticApplication.class, "Timing send: ", avg.flushInstance());
                }
            }).start();
        }
    }


}
