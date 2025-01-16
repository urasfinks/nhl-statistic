package ru.jamsys.core.component;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Lazy
public class RateLimit {

    private final AtomicInteger countRequest = new AtomicInteger(0);

    private final AtomicBoolean rateLimitFinish = new AtomicBoolean(false);

    int maxCountRequest = 15_000;

    public boolean isRateLimit() {
        int c = countRequest.incrementAndGet();
        boolean result = c <= maxCountRequest;
        if (!result) {
            if (rateLimitFinish.compareAndSet(false, true)) {
                TelegramBotComponent telegramBotComponent = App.get(TelegramBotComponent.class);
                if (telegramBotComponent.getNhlStatisticsBot() != null) {
                    App.get(TelegramQueueSender.class).add(
                            telegramBotComponent.getNhlStatisticsBot(),
                            //290029195, // urasfinks
                            -4739098379L, // NhlCommon
                            "RateLimit 15_000 Finish",
                            null,
                            null
                    );
                }
            }
        }
        return result;
    }

    public void reset() {
        countRequest.set(0);
        rateLimitFinish.set(false);
    }

}
