package ru.jamsys;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import ru.jamsys.core.App;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.resource.http.client.HttpClientImpl;
import ru.jamsys.core.resource.http.client.HttpResponse;
import ru.jamsys.core.statistic.AvgMetric;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

@SpringBootApplication
public class NhlStatisticApplication {

    public static boolean startTelegramListener = true; // Запускать чтение сообщений от бота

    public static void main(String[] args) {
        App.springSource = NhlStatisticApplication.class;
        App.main(args);
    }

    @SuppressWarnings("unused")
    public static void loadTelegram(){
        Queue<String> queue = new ConcurrentLinkedQueue<>();
        for (int i = 0; i < 50; i++) {
            queue.add(i + "");
        }
        long start = System.currentTimeMillis();
        AvgMetric avg = new AvgMetric();
        AtomicBoolean a = new AtomicBoolean(false);
        for (int i = 0; i < 1; i++) {
            new Thread(() -> {
                while (!queue.isEmpty()) {
                    String poll = queue.poll();
                    if (poll != null) {
                        HttpResponse send = send(poll);
                        avg.add(send.getTiming());
                        Util.logConsoleJson(send);
                    }
                }
                Util.logConsole("Timing send: " + (System.currentTimeMillis() - start));
                if (a.compareAndSet(false, true)) {
                    Util.sleepMs(5000);
                    Util.logConsoleJson(avg.flushInstance());
                }
            }).start();
        }
    }

    public static HttpResponse send(String data) {
        HttpClientImpl httpClient = new HttpClientImpl();
        httpClient.setUrl(String.format(
                "https://api.telegram.org/bot%s/sendMessage?parse_mode=markdown&chat_id=%s&text=%s",
                "",
                290029195L,
                URLEncoder.encode(data, StandardCharsets.UTF_8))
        );
        httpClient.setTimeoutMs(10_000);
        httpClient.exec();
        return httpClient.getHttpResponseEnvelope();
    }

}
