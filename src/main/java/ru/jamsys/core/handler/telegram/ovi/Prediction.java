package ru.jamsys.core.handler.telegram.ovi;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.NhlStatisticApplication;
import ru.jamsys.core.App;
import ru.jamsys.core.component.Chart;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.builder.ArrayListBuilder;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.flat.template.twix.TemplateTwix;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilNHL;
import ru.jamsys.core.flat.util.UtilRisc;
import ru.jamsys.core.flat.util.UtilTrend;
import ru.jamsys.core.handler.promise.PlayerStatistic;
import ru.jamsys.core.handler.promise.RegisterNotification;
import ru.jamsys.core.handler.promise.Tank01Request;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.tank.data.NHLGamesForPlayer;
import ru.jamsys.tank.data.NHLTeamSchedule;
import ru.jamsys.telegram.TelegramCommandContext;
import ru.jamsys.telegram.TelegramNotification;
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
                    // -- Статистика Ови ----
                    TelegramCommandContext context = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    PlayerStatistic ovi = promise.getRepositoryMapClass(Promise.class, "ovi")
                            .getRepositoryMapClass(PlayerStatistic.class);
                    if (ovi.getOffsetGretzky() <= 0) {
                        //TODO: всё
                        return;
                    }
                    // ----------------
                    Tank01Request tank01Request = promise
                            .getRepositoryMapClass(Promise.class, "requestGamesForPlayer")
                            .getRepositoryMapClass(Tank01Request.class);

                    // -- Игры сезона --
                    Tank01Request response = promise
                            .getRepositoryMapClass(Promise.class, "requestGameInSeason")
                            .getRepositoryMapClass(Tank01Request.class);
                    NHLTeamSchedule.Instance instance = new NHLTeamSchedule.Instance(response.getResponseData());
                    List<String> lisIdGameInSeason = new ArrayList<>(instance.getIdGame());
                    // -----------------
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

                    List<NHLTeamSchedule.Game> listGameInstance = instance
                            .initAlreadyGame()
                            .getFutureGame()
                            .getListGameInstance();

                    String templateInGame = "🔹 Рекорд, скорее всего, устоит в этом сезоне";
                    if (listGameInstance.size() >= seasonChart.getCountGame()) {
                        NHLTeamSchedule.Game game = listGameInstance.get(seasonChart.getCountGame() - 1);
                        templateInGame = String.format(
                                """
                                        📅 Возможная ключевая игра: 🆚 %s
                                        🕑 Дата и время: %s (МСК)""",
                                game.toggleTeam(UtilNHL.getOvi().getTeam()),
                                game.getMoscowDate()
                        );
                    }

                    String templateInGame2 = "🔹 Рекорд, скорее всего, устоит в этом сезоне";
                    if (listGameInstance.size() >= allChart.getCountGame()) {
                        NHLTeamSchedule.Game game = listGameInstance.get(allChart.getCountGame() - 1);
                        templateInGame = String.format(
                                """
                                        📅 Возможная ключевая игра: 🆚 %s
                                        🕑 Дата и время: %s (МСК)""",
                                game.toggleTeam(UtilNHL.getOvi().getTeam()),
                                game.getMoscowDate()
                        );
                    }
                    List<TelegramNotification> telegramNotifications = new ArrayListBuilder<TelegramNotification>()
                            .append(new TelegramNotification(
                                    context.getIdChat(),
                                    context.getTelegramBot().getBotUsername(),
                                    null,
                                    null,
                                    "file:/" + allChart.getPathChart()
                            ))
                            .append(new TelegramNotification(
                                    context.getIdChat(),
                                    context.getTelegramBot().getBotUsername(),
                                    null,
                                    null,
                                    "file:/" + seasonChart.getPathChart()
                            ))
                            .append(new TelegramNotification(
                                    context.getIdChat(),
                                    context.getTelegramBot().getBotUsername(), //Сезон : ${countGame} ${countGamePostfix}, ${seasonGoals} ${seasonGoalsPostfix}
                                    TemplateTwix.template("""
                                            🏆 Охота за рекордом Гретцки
                                            
                                            📊 По анализу сезона ${seasonTitle}:
                                            ${gameAbout}
                                            
                                            📊 По анализу последних ${allCountGame} ${allCountGamePostfix}:
                                            ${gameAbout2}
                                            
                                            🤖 Мы тут прикинули — Александру потребуется еще ${seasonPrediction}–${allPrediction} ${predictionPostfix}, чтобы вписать свое имя в историю.
                                            
                                            """, new HashMapBuilder<String, String>()
                                            .append("seasonTitle", UtilNHL.seasonFormat(UtilNHL.getActiveSeasonOrNext()))
                                            .append("countGame", String.valueOf(seasonChart.getInitGame()))
                                            .append("countGamePostfix", Util.digitTranslate(seasonChart.getInitGame(), "матч", "матча", "матчей"))
                                            .append("seasonGoals", String.valueOf(seasonChart.getInitCountGoals()))
                                            .append("seasonGoalsPostfix", Util.digitTranslate(seasonChart.getInitCountGoals(), "гол", "гола", "голов"))
                                            .append("gameAbout", templateInGame)
                                            .append("allCountGame", String.valueOf(allChart.getInitGame()))
                                            .append("allCountGamePostfix", Util.digitTranslate(allChart.getInitGame(), "матч", "матча", "матчей"))
                                            .append("gameAbout2", templateInGame2)
                                            .append("seasonPrediction", String.valueOf(seasonChart.getCountGame()))
                                            .append("allPrediction", String.valueOf(allChart.getCountGame()))
                                            .append("predictionPostfix", Util.digitTranslate(allChart.getCountGame(), "матч", "матча", "матчей"))
                                    ),
                                    null,
                                    null
                            ));

                    RegisterNotification.add(telegramNotifications);
                })
                .extension(NhlStatisticApplication::addOnError);
    }

}
