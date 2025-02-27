package ru.jamsys.core.handler.telegram.ovi;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.NhlStatisticApplication;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.handler.promise.RegisterNotification;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.telegram.TelegramCommandContext;
import ru.jamsys.telegram.TelegramNotification;
import ru.jamsys.telegram.handler.OviGoalsBotCommandHandler;

@SuppressWarnings("unused")
@Setter
@Getter
@Component
@RequestMapping("/all")
public class All implements PromiseGenerator, OviGoalsBotCommandHandler {

    private final ServicePromise servicePromise;

    public All(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(getClass().getSimpleName(), 12_000L)
                .extension(promise -> promise.setRepositoryMapClass(All.class, this))
                .then("send", (_, _, promise) -> {
                            TelegramCommandContext context = promise.getRepositoryMapClass(TelegramCommandContext.class);
                            RegisterNotification.add(new TelegramNotification(
                                    context.getIdChat(),
                                    context.getTelegramBot().getBotUsername(),
                                    """
                                            /start — Включить уведомления
                                            /stats — Текущая статистика: количество голов, оставшихся до рекорда, и статистика по сезону
                                            /poll_results — Статистика голосования
                                            /schedule — Ближайшие игры Александра Овечкина и команды Washington Capitals
                                            /prediction — Когда Овечкин побьет рекорд Гретцки?
                                            /quiz — Насколько хорошо ты знаешь Александра Овечкина?
                                            /bets — Ставки на Овечкина
                                            /stop — Отключить уведомления""",
                                    null,
                                    null
                            ));
                        }

                )
                .extension(NhlStatisticApplication::addOnError);
    }

}
