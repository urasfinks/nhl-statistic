package ru.jamsys.core.component.cron;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.component.TelegramBotManager;
import ru.jamsys.core.component.manager.item.Session;
import ru.jamsys.core.extension.UniqueClassName;
import ru.jamsys.core.flat.template.cron.release.Cron1s;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.flat.util.UtilTelegramResponse;
import ru.jamsys.core.flat.util.telegram.Button;
import ru.jamsys.core.jt.JTTelegramSend;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;
import ru.jamsys.telegram.TelegramNotification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("unused")
@Component
@Lazy
public class SecScheduler implements Cron1s, PromiseGenerator, UniqueClassName {

    private final ServicePromise servicePromise;

    private final ServiceProperty serviceProperty;

    private final Session<Long, AtomicInteger> session = new Session<>("RateLimitTelegramSend", 600_000L);

    private static final AtomicInteger countThread = new AtomicInteger(0);

    // TelegramApi блокирует бота если в несколько потоков посылать (проверено)
    // WebHook норм переваривает много потоков
    private static final int maxThread = 10;

    public SecScheduler(ServicePromise servicePromise, ServiceProperty serviceProperty) {
        this.servicePromise = servicePromise;
        this.serviceProperty = serviceProperty;
    }

    public Promise generate() {
        return servicePromise.get(getClass().getSimpleName(), 6000_000L)
                .then("check", (atomicBoolean, promiseTask, promise) -> {
                    String mode = serviceProperty.get(String.class, "run.mode", "test");
                    if (mode.equals("test")) { // Если запущены в режиме тест - не надо ничего делать
                        promise.skipAllStep("mode test");
                        return;
                    }
                    session.forEach((_, atomicInteger) -> atomicInteger.set(0));
                    if (App.get(TelegramBotManager.class).getRepository().size() < 4) {
                        promise.skipAllStep("size bot < 4");
                    }
                })
                .thenWithResource("select", JdbcResource.class, (isRun, _, _, jdbcResource) -> {
                    if (countThread.get() >= maxThread) {
                        return;
                    }
                    int currentCountThread = countThread.incrementAndGet();
                    //Util.logConsole(getClass(), "Init thread: " + currentCountThread);
                    int countLoop = 0;
                    List<String> listBotName = App.get(TelegramBotManager.class).getListBotName();
                    TelegramBotManager telegramBotManager = App.get(TelegramBotManager.class);
                    while (isRun.get()) {
                        try {
                            List<JTTelegramSend.Row> execute = jdbcResource
                                    .execute(new JdbcRequest(JTTelegramSend.SELECT_ONE)
                                                    .addArg("bots", listBotName)
                                                    .setDebug(false),
                                            JTTelegramSend.Row.class);
                            if (execute.isEmpty()) {
                                break;
                            }
                            for (int i = currentCountThread; i < maxThread; i++) {
                                this.generate().run();
                            }
                            JTTelegramSend.Row first = execute.getFirst();
                            long idChat = Long.parseLong(first.getIdChat().toString());

                            AtomicInteger rateLimit = session.computeIfAbsent(idChat, _ -> new AtomicInteger(0));
                            int rateLimitTotal = rateLimit.incrementAndGet();
                            if (rateLimitTotal >= 30) {
                                Util.logConsole(getClass(), "RateLimit(" + rateLimitTotal + ") by idChat: " + idChat);
                                Util.sleepMs(1000);
                            }
                            TelegramNotification telegramNotification = new TelegramNotification(
                                    idChat,
                                    first.getBot(),
                                    first.getMessage(),
                                    parseButton(first.getButtons()),
                                    first.getPathImage()
                            );
                            telegramNotification.setIdImage(first.getIdImage());
                            telegramNotification.setIdVideo(first.getIdVideo());

                            UtilTelegramResponse.Result send = telegramBotManager.send(
                                    telegramNotification,
                                    TelegramBotManager.TypeSender.HTTP
                            );

                            if (send.isRetry()) {
                                jdbcResource.execute(new JdbcRequest(JTTelegramSend.SEND_RETRY)
                                        .addArg("id", first.getId())
                                        .addArg("json", UtilJson.toStringPretty(send, "{}"))
                                );
                            } else {
                                jdbcResource.execute(new JdbcRequest(JTTelegramSend.SEND_FINISH)
                                        .addArg("id", first.getId())
                                        .addArg("json", UtilJson.toStringPretty(send, "{}"))
                                );
                            }
                        } catch (Throwable th) {
                            App.error(th);
                        } finally {
                            // В любом случае надо отпустить блокировку, даже если сломались
                            jdbcResource.execute(new JdbcRequest(JTTelegramSend.COMMIT));
                        }
                        countLoop++;
                        if (countLoop > 1000) {
                            Util.logConsole(getClass(), "loop break 1000");
                            break;
                        }
                    }
                    countThread.decrementAndGet();
                })
                .setDebug(false);
    }

    public static List<Button> parseButton(String data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        List<Button> result = new ArrayList<>();
        try {
            List<Object> listOrThrow = UtilJson.getListOrThrow(data);
            listOrThrow.forEach(o -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> o1 = (Map<String, Object>) o;
                Button btn = new Button(o1.get("data").toString());

                if (o1.containsKey("callback") && o1.get("callback") != null) {
                    btn.setCallback(o1.get("callback").toString());
                }
                if (o1.containsKey("url") && o1.get("url") != null) {
                    btn.setUrl(o1.get("url").toString());
                }
                result.add(btn);
            });
        } catch (Throwable th) {
            App.error(th);
        }
        return result.isEmpty() ? null : result;
    }

}
