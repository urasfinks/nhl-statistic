package ru.jamsys.core.handler.telegram.ovi;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.flat.util.UtilListSort;
import ru.jamsys.core.flat.util.UtilNHL;
import ru.jamsys.core.handler.promise.Tank01Request;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.tank.data.NHLTeamSchedule;
import ru.jamsys.telegram.AbstractBot;
import ru.jamsys.telegram.TelegramCommandContext;
import ru.jamsys.telegram.handler.OviGoalsBotCommandHandler;

@SuppressWarnings("unused")
@Setter
@Getter
@Component
@RequestMapping({"/schedule"})
public class Schedule implements PromiseGenerator, OviGoalsBotCommandHandler {

    private final ServicePromise servicePromise;

    public Schedule(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(getClass().getSimpleName(), 12_000L)
                .extension(promise -> promise.setRepositoryMapClass(Schedule.class, this))
                .then("requestGameInSeason", new Tank01Request(() -> NHLTeamSchedule.getUri(
                        "31",
                        UtilNHL.getActiveSeasonOrNext() + ""
                )).generate())
                .then("parseGameInSeason", (_, _, promise) -> {
                    TelegramCommandContext context = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    Tank01Request response = promise
                            .getRepositoryMapClass(Promise.class, "requestGameInSeason")
                            .getRepositoryMapClass(Tank01Request.class);
                    NHLTeamSchedule.Instance instance = new NHLTeamSchedule.Instance(response.getResponseData())
                            .getScheduledAndLive()
                            .getFutureGame()
                            .sort(UtilListSort.Type.ASC);
                    StringBuilder sb = new StringBuilder();
                    instance.getListGameObject().forEach(game -> {
                        sb.append(String.format("""
                                        %s — 🆚 %s, %s (GMT+03:00)
                                        """,
                                game.getMoscowDate("dd.MM.yyyy"),
                                game.toggleTeam("WSH"),
                                game.getMoscowDate("HH:mm")
                        )).append("\n");
                    });
                    AbstractBot.splitMessageSmart(String.format("""
                                            📅 Расписание ближайших игр Александра Овечкина и Washington Capitals
                                            
                                            %s
                                            
                                            📍 Время начала игр указано по МСК (GMT+03:00)
                                            """,
                                    sb
                            ), 3000)
                            .forEach(s -> context.getTelegramBot().send(context.getIdChat(), s, null));
                })
                ;
    }

}
