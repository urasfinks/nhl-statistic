package ru.jamsys.core.component.cron;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.TelegramQueueSender;
import ru.jamsys.core.extension.UniqueClassName;
import ru.jamsys.core.flat.template.cron.release.Cron1s;
import ru.jamsys.core.handler.promise.SendNotification;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.telegram.NotificationObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

@SuppressWarnings("unused")
@Component
@Lazy
public class SecScheduler implements Cron1s, PromiseGenerator, UniqueClassName {

    private final ServicePromise servicePromise;

    public SecScheduler(
            ServicePromise servicePromise
    ) {
        this.servicePromise = servicePromise;
    }

    public Promise generate() {
        return servicePromise.get(getClass().getSimpleName(), 10_000L)
                .then("readQueueSend", (atomicBoolean, promiseTask, promise) -> {
                    ConcurrentLinkedQueue<NotificationObject> queue = App.get(TelegramQueueSender.class).getQueue();
                    int count = 0;
                    List<NotificationObject> listTelegramContext = new ArrayList<>();
                    while (!queue.isEmpty()) {
                        if (count > 29) {
                            break;
                        }
                        NotificationObject poll = queue.poll();
                        if (poll == null) {
                            continue;
                        }
                        listTelegramContext.add(poll);
                        count++;
                    }
                    listTelegramContext.forEach(context -> new SendNotification(context).generate().run());
                })
                .setDebug(false);
    }

}
