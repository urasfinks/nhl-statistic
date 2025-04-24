package ru.jamsys.core.component.cron;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.RateLimitNhlApi;
import ru.jamsys.core.extension.UniqueClassName;
import ru.jamsys.core.flat.template.cron.Cron;
import ru.jamsys.core.flat.template.cron.release.CronConfigurator;
import ru.jamsys.core.handler.promise.UpdateScheduler;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;

@SuppressWarnings("unused")
@Component
@Lazy
public class DayScheduler implements CronConfigurator, PromiseGenerator, UniqueClassName {

    public Promise generate() {
        App.get(RateLimitNhlApi.class).reset();
        return new UpdateScheduler(true).generate();
    }

    @Override
    public boolean isTimeHasCome(Cron.CompileResult compileResult) {
        //return true;
        return compileResult.getBeforeTimestamp() != 0;
    }

    @Override
    public String getCronTemplate() {
        return "0 0 12 * *"; //  В 12:00:00 каждого любого дня месяца/года
    }

}
