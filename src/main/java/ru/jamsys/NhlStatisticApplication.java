package ru.jamsys;

import lombok.Getter;
import lombok.Setter;
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

    @Getter
    @Setter
    public static class MSG{
        final String data;
        final Long idChat;

        public MSG(String data, Long idChat) {
            this.data = data;
            this.idChat = idChat;
        }
    }

    @SuppressWarnings("unused")
    public static void loadTelegram(){
        Queue<MSG> queue = new ConcurrentLinkedQueue<>();
        for (int i = 0; i < 30; i++) {
            queue.add(new MSG(i + "", 290029195L));
            queue.add(new MSG(i + "", 294097034L));
            queue.add(new MSG(i + "", 241022301L));
        }
        long start = System.currentTimeMillis();
        AvgMetric avg = new AvgMetric();
        AtomicBoolean a = new AtomicBoolean(false);
        AtomicBoolean isRun = new AtomicBoolean(true);
        for (int i = 0; i < 1; i++) {
            new Thread(() -> {
                while (isRun.get()) {
                    MSG poll = queue.poll();
                    if (poll != null) {
                        HttpResponse send = send(poll.getIdChat(), poll.getData());
                        avg.add(send.getTiming());
                        //Util.logConsoleJson(send);
                    }else{
                        Util.sleepMs(1000);
                    }
                }
                //Util.logConsole("Timing send: " + (System.currentTimeMillis() - start));
                if (a.compareAndSet(false, true)) {
                    Util.sleepMs(5000);
                    Util.logConsoleJson(avg.flushInstance());
                }
            }).start();
        }
    }

    public static HttpResponse send(long idChat, String data) {
        HttpClientImpl httpClient = new HttpClientImpl();
        httpClient.setUrl(String.format(
                "https://api.telegram.org/bot%s/sendMessage?parse_mode=markdown&chat_id=%s&text=%s",
                //"http://176.124.217.254:8081/bot%s/sendMessage?parse_mode=markdown&chat_id=%s&text=%s",
                "",
                idChat,
                URLEncoder.encode(data, StandardCharsets.UTF_8))
        );
        httpClient.setTimeoutMs(10_000);
        httpClient.exec();
        return httpClient.getHttpResponseEnvelope();
    }

}
