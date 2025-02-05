package ru.jamsys.core.handler.telegram.ovi;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.NhlStatisticApplication;
import ru.jamsys.core.App;
import ru.jamsys.core.component.Chart;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.flat.util.UtilNHL;
import ru.jamsys.core.flat.util.UtilRisc;
import ru.jamsys.core.flat.util.UtilTrend;
import ru.jamsys.core.handler.promise.PlayerStatistic;
import ru.jamsys.core.handler.promise.Tank01Request;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.tank.data.NHLGamesForPlayer;
import ru.jamsys.tank.data.NHLTeamSchedule;
import ru.jamsys.telegram.TelegramCommandContext;
import ru.jamsys.telegram.handler.OviGoalsBotCommandHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("unused")
@Setter
@Getter
@Component
@RequestMapping("/prediction")
public class Prediction implements PromiseGenerator, OviGoalsBotCommandHandler {

    private final ServicePromise servicePromise;

    public Prediction(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(getClass().getSimpleName(), 12_000L)
                .then("ovi", new PlayerStatistic(UtilNHL.getOvi(), UtilNHL.getOviScoreLastSeason()).generate())
                .then("requestGameInSeason", new Tank01Request(() -> NHLTeamSchedule.getUri(
                        UtilNHL.getOvi().getTeamID(),
                        UtilNHL.getActiveSeasonOrNext() + ""
                )).generate())
                .then("requestGamesForPlayer", new Tank01Request(() -> NHLGamesForPlayer.getUri(
                        UtilNHL.getOvi().getPlayerID()
                )).generate())
                .then("send", (run, _, promise) -> {
                    // -- Игры сезона --
                    Tank01Request response = promise
                            .getRepositoryMapClass(Promise.class, "requestGameInSeason")
                            .getRepositoryMapClass(Tank01Request.class);
                    NHLTeamSchedule.Instance instance = new NHLTeamSchedule.Instance(response.getResponseData());
                    List<String> lisIdGameInSeason = new ArrayList<>(instance.getIdGame());
                    // -----------------
                    // -- Статистика Ови ----
                    TelegramCommandContext context = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    PlayerStatistic ovi = promise.getRepositoryMapClass(Promise.class, "ovi")
                            .getRepositoryMapClass(PlayerStatistic.class);
                    //Util.logConsoleJson(getClass(), ovi);
                    Tank01Request tank01Request = promise
                            .getRepositoryMapClass(Promise.class, "requestGamesForPlayer")
                            .getRepositoryMapClass(Tank01Request.class);
                    // ----------------

                    UtilTrend.XY allXy = new UtilTrend.XY();
                    AtomicInteger allCountGoals = new AtomicInteger(0);
                    Map<String, Integer> mapAllGoals = NHLGamesForPlayer
                            .getOnlyGoalsFilter(tank01Request.getResponseData(), null);
                    UtilRisc.forEach(
                            run,
                            mapAllGoals.keySet(),
                            (idGame) -> {
                                allXy.addY(allCountGoals.addAndGet(mapAllGoals.get(idGame)));
                            },
                            true
                    );
                    Chart.Response allChart = App.get(Chart.class).createChart(allXy, ovi.getOffsetGretzky());
                    //Util.logConsoleJson(getClass(), allChart);
                    // ----------------
                    UtilTrend.XY seasonXy = new UtilTrend.XY();
                    AtomicInteger seasonCountGoals = new AtomicInteger(0);
                    Map<String, Integer> mapSeasonGoals = NHLGamesForPlayer
                            .getOnlyGoalsFilter(tank01Request.getResponseData(), lisIdGameInSeason);
                    UtilRisc.forEach(
                            run,
                            mapSeasonGoals.keySet(),
                            (idGame) -> {
                                seasonXy.addY(seasonCountGoals.addAndGet(mapSeasonGoals.get(idGame)));
                            },
                            true
                    );
                    Chart.Response seasonChart = App.get(Chart.class).createChart(seasonXy, ovi.getOffsetGretzky());
                    //Util.logConsoleJson(getClass(), lisIdGameInSeason);
                    //Util.logConsoleJson(getClass(), seasonChart);
                    //{
                    //  "pathChart" : "/Users/sfinks/IdeaProjects/nhl-statistic/chart/chart_16_37.png",
                    //  "countGame" : 22,
                    //  "needCountGoals" : 41,
                    //  "initGame" : 37,
                    //  "initCountGoals" : 25
                    //}
                })
                .extension(NhlStatisticApplication::addOnError);
    }

}
