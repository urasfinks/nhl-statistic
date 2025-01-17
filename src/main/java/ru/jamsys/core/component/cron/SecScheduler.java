package ru.jamsys.core.component.cron;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.UniqueClassName;
import ru.jamsys.core.flat.template.cron.release.Cron1s;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.flat.util.telegram.Button;
import ru.jamsys.core.handler.promise.SendNotification;
import ru.jamsys.core.jt.JTTelegramSend;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;
import ru.jamsys.telegram.AbstractBot;
import ru.jamsys.telegram.NotificationObject;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("unused")
@Component
@Lazy
public class SecScheduler implements Cron1s, PromiseGenerator, UniqueClassName {

    private final ServicePromise servicePromise;

    private static final AtomicInteger countThread = new AtomicInteger(0);

    private static final int maxThread = 3;

    public SecScheduler(
            ServicePromise servicePromise
    ) {
        this.servicePromise = servicePromise;
    }

    public Promise generate() {
        System.out.println("GEN " + getClassName());
        return servicePromise.get(getClass().getSimpleName(), 10_000L)
                .then("test", (atomicBoolean, promiseTask, promise) -> {
                    //TODO без этой задачи следующая задача не вызывается - надо разобраться почему
                })
                .thenWithResource("select", JdbcResource.class, (_, _, _, jdbcResource) -> {
                    //Util.logConsoleJson(App.get(ServiceProperty.class).getProp());
                    //Util.logConsole(">>");
                    if (countThread.get() >= maxThread) {
                        return;
                    }
                    countThread.incrementAndGet();
                    Util.logConsole("SecScheduler thread: " + countThread.get());
                    int countLoop = 0;
                    while (true) {
                        try {
                            List<JTTelegramSend.Row> execute = jdbcResource
                                    .execute(new JdbcRequest(JTTelegramSend.SELECT_ONE), JTTelegramSend.Row.class);
                            if (execute.isEmpty()) {
                                Util.logConsole("SecScheduler.loop break empty");
                                break;
                            }
                            JTTelegramSend.Row first = execute.getFirst();

                            List<Button> listResult = null;
                            if (first.getButtons() != null) {
                                @SuppressWarnings("unchecked")
                                List<Button> list = (List<Button>) Util.mapToObject(UtilJson.getMapOrThrow(first.getButtons()), List.class);
                                listResult = list;
                            }

                            AbstractBot.TelegramResult send = SendNotification.send(new NotificationObject(
                                    Long.parseLong(first.getIdChat().toString()),
                                    first.getBot(),
                                    first.getMessage(),
                                    listResult,
                                    first.getPathImage()
                            ));
                            Util.logConsoleJson(send);
                            if (send.isRetry()) {
                                jdbcResource.execute(new JdbcRequest(JTTelegramSend.SEND_ERROR)
                                        .addArg("id", first.getId())
                                        .addArg("json", UtilJson.toStringPretty(send, "{}"))
                                );
                            }else{
                                jdbcResource.execute(new JdbcRequest(JTTelegramSend.SEND_SUCCESS)
                                        .addArg("id", first.getId())
                                        .addArg("json", UtilJson.toStringPretty(send, "{}"))
                                );
                            }
                        } finally {
                            // В любом случае надо отпустить блокировку, даже если сломались
                            jdbcResource.execute(new JdbcRequest(JTTelegramSend.COMMIT));
                        }
                        countLoop++;
                        if (countLoop > 300) {
                            Util.logConsole("SecScheduler.loop break 300");
                            break;
                        }
                    }
                    countThread.decrementAndGet();
                })

                .setDebug(false);
    }

}
