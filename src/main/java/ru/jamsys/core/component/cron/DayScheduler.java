package ru.jamsys.core.component.cron;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.extension.UniqueClassName;
import ru.jamsys.core.flat.template.cron.release.CronTemplate;
import ru.jamsys.core.handler.promise.UpdateScheduler;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;

@SuppressWarnings("unused")
@Component
@Lazy
public class DayScheduler implements CronTemplate, PromiseGenerator, UniqueClassName {

    public Promise generate() {
        return new UpdateScheduler(true).generate();
    }

    @Override
    public String getCronTemplate() {
        return "0 0 12 * *"; //  В 12:00:00 каждого любого дня месяца/года
    }

}
