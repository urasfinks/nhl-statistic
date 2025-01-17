package ru.jamsys.core.component.cron;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.UniqueClassName;
import ru.jamsys.core.flat.template.cron.release.Cron1s;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;

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
                })
                .setDebug(false);
    }

}
