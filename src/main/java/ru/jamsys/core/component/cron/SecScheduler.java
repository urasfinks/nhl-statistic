package ru.jamsys.core.component.cron;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.TelegramBotManager;
import ru.jamsys.core.extension.UniqueClassName;
import ru.jamsys.core.flat.template.cron.release.Cron1s;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.flat.util.UtilTelegram;
import ru.jamsys.core.flat.util.telegram.Button;
import ru.jamsys.core.jt.JTTelegramSend;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("unused")
@Component
@Lazy
public class SecScheduler implements Cron1s, PromiseGenerator, UniqueClassName {

    private final ServicePromise servicePromise;

    private static final AtomicInteger countThread = new AtomicInteger(0);

    private static final int maxThread = 1; // TelegramApi блокирует бота если в несколько потоков посылать (проверено)

    public SecScheduler(
            ServicePromise servicePromise
    ) {
        this.servicePromise = servicePromise;
    }

    public Promise generate() {
        return servicePromise.get(getClass().getSimpleName(), 6000_000L)
                .then("check", (atomicBoolean, promiseTask, promise) -> {
                    if (App.get(TelegramBotManager.class).getRepository().size() < 4) {
                        promise.skipAllStep("size bot < 4");
                    }
                })
                .thenWithResource("select", JdbcResource.class, (isRun, _, _, jdbcResource) -> {
                    if (countThread.get() >= maxThread) {
                        return;
                    }
                    countThread.incrementAndGet();
                    int countLoop = 0;
                    List<String> listBotName = App.get(TelegramBotManager.class).getListBotName();
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
                            JTTelegramSend.Row first = execute.getFirst();
                            // TODO:
//                            UtilTelegram.Result send = SendNotification.send(new NotificationObject(
//                                    Long.parseLong(first.getIdChat().toString()),
//                                    first.getBot(),
//                                    first.getMessage(),
//                                    parseButton(first.getButtons()),
//                                    first.getPathImage()
//                            ));
                            // TODO: remove
                            UtilTelegram.Result send = new UtilTelegram.Result();
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
                        } catch (Throwable th){
                            App.error(th);
                        } finally {
                            // В любом случае надо отпустить блокировку, даже если сломались
                            jdbcResource.execute(new JdbcRequest(JTTelegramSend.COMMIT));
                        }
                        countLoop++;
                        if (countLoop > 300) {
                            Util.logConsole(getClass(), "loop break 300");
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
                result.add(new Button(o1.get("data").toString(), o1.get("callback").toString()));
            });
        } catch (Throwable th) {
            App.error(th);
        }
        return result.isEmpty() ? null : result;
    }

}
