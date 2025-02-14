package ru.jamsys.core.component.cron;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.UniqueClassName;
import ru.jamsys.core.flat.template.cron.release.Cron30m;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;

@SuppressWarnings("unused")
@Component
@Lazy
public class Min30Scheduler implements Cron30m, PromiseGenerator, UniqueClassName {

    private final ServicePromise servicePromise;

    public Min30Scheduler(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    public Promise generate() {
        return servicePromise.get(getClass().getSimpleName(), 6_000L)
                .then("YandexToken", (atomicBoolean, promiseTask, promise) -> {
                    //new YandexTokenRequest().generate().run();
                });
    }


}
