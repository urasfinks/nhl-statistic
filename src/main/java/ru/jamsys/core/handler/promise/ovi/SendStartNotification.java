package ru.jamsys.core.handler.promise.ovi;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.DelaySenderComponent;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.handler.promise.PlayerStatistic;
import ru.jamsys.core.handler.promise.PlayerStatisticOvi;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.telegram.TelegramCommandContext;

@Getter
@Setter
public class SendStartNotification implements PromiseGenerator {

    private TelegramCommandContext context;

    public SendStartNotification(TelegramCommandContext context) {
        this.context = context;
    }

    @Override
    public Promise generate() {
        return App.get(ServicePromise.class).get(getClass().getSimpleName(), 60_000L)
                .then("ovi", new PlayerStatisticOvi().generate())
                .then("send", (_, _, promise) -> {
                    PlayerStatistic ovi = promise.getRepositoryMapClass(Promise.class, "ovi")
                            .getRepositoryMapClass(PlayerStatistic.class);
                    context.getTelegramBot().send(
                            context.getIdChat(),
                            String.format("""
                                            Саня снова в деле! Включи уведомления, чтобы не пропустить ни одного гола.
                                            
                                            Поддержим Ови на пути к величию! 🏒🔥
                                            
                                            %s
                                            """,
                                    ovi.getMessage()
                            ),
                            null
                    );
                    App.get(DelaySenderComponent.class).add(context, """
                            Ты также можешь воспользоваться дополнительными командами:
                            
                            /stats — Текущая статистика: количество голов, оставшихся до рекорда, и статистика по сезону.
                            /schedule — Ближайшие игры Александра Овечкина и команды Washington Capitals.
                            /stop — Отключить уведомления
                            """, 10_000L);
                })
                .setDebug(true)
                ;
    }

}
