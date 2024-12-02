package ru.jamsys.core.component.cron;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.UniqueClassName;
import ru.jamsys.core.flat.template.cron.release.Cron1m;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;

@Component
@Lazy
public class MinScheduler implements Cron1m, PromiseGenerator, UniqueClassName {

    private final ServicePromise servicePromise;

    @Setter
    @Getter
    private String index;

    public MinScheduler(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(index, 6_000L)
                .append("main", (_, _, _) -> {
                    //Util.logConsole("Hello");
                });
    }

}
