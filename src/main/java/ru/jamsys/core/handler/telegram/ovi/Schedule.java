package ru.jamsys.core.handler.telegram.ovi;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.builder.ArrayListBuilder;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.http.ServletResponseWriter;
import ru.jamsys.core.flat.util.Paginator;
import ru.jamsys.core.flat.util.UtilListSort;
import ru.jamsys.core.flat.util.UtilNHL;
import ru.jamsys.core.flat.util.telegram.Button;
import ru.jamsys.core.handler.promise.Tank01Request;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.tank.data.NHLTeamSchedule;
import ru.jamsys.telegram.TelegramCommandContext;
import ru.jamsys.telegram.handler.OviGoalsBotCommandHandler;

import java.util.List;

@SuppressWarnings("unused")
@Setter
@Getter
@Component
@RequestMapping({"/schedule/**"})
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
                            .initAlreadyGame()
                            .getFutureGame()
                            .sort(UtilListSort.Type.ASC);

                    int page = Integer.parseInt(context.getUriParameters().getOrDefault("page", "1"));
                    paging(instance.getListGameObject(), page, context, """
                            📅 Расписание ближайших игр Александра Овечкина и Washington Capitals (WSH)
                            
                            %s
                            
                            📍 Время начала игр указано по МСК
                            """);
                })
                ;
    }

    public static void paging(List<NHLTeamSchedule.Game> listGame, int page, TelegramCommandContext context, String titleTemplate) {
        StringBuilder sb = new StringBuilder();
        Paginator<NHLTeamSchedule.Game> paginator = new Paginator<>(listGame, 10);
        paginator.getPage(page).forEach(game -> sb.append(String.format("""
                        %s — 🆚 %s, %s
                        """,
                game.getMoscowDate("dd.MM.yyyy"),
                game.toggleTeam(UtilNHL.getOvi().getTeam()),
                game.getMoscowDate("HH:mm")
        )).append("\n"));

        List<Button> list = null;
        if (paginator.getNextPage(page) != null) {
            list = new ArrayListBuilder<Button>().append(new Button(
                    "Дальше",
                    ServletResponseWriter.buildUrlQuery(
                            context.getUriPath() + "/",
                            new HashMapBuilder<>(context.getUriParameters())
                                    .append("page", paginator.getNextPage(page).toString())

                    )
            ));
        }
        if (page == 1) {
            context.getTelegramBot().send(context.getIdChat(), String.format(titleTemplate, sb), list);
        } else {
            context.getTelegramBot().send(context.getIdChat(), sb.toString(), list);
        }
    }

}
