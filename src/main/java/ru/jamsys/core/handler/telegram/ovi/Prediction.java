package ru.jamsys.core.handler.telegram.ovi;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
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
                        RegisterNotification.add(new TelegramNotification(
                                context.getIdChat(),
                                context.getTelegramBot().getBotUsername(),
                                """
                                        🎉 Рекорд Гретцки побит! 🎉
                                        Спасибо, что наблюдали за этим величайшим событием вместе с нами 🏒✨
                                        """,
                                null,
                                null
                        ));
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
                    Chart.Response seasonChart = App.get(Chart.class)
                            .createChart(seasonXy, ovi.getOffsetGretzky(), false);

                    List<NHLTeamSchedule.Game> listGameInstance = instance
                            .initAlreadyGame()
                            .getFutureGame()
                            .getListGameInstance();

                    List<TelegramNotification> telegramNotifications = new ArrayListBuilder<TelegramNotification>()
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
                                            
                                            📈 По анализу сезона ${seasonTitle} потребуется еще ${count} ${countPostfix}
                                            
                                            ${gameAbout}
                                            
                                            """, new HashMapBuilder<String, String>()
                                            .append("seasonTitle", UtilNHL.seasonFormat(UtilNHL.getActiveSeasonOrNext()))
                                            .append("gameAbout", getString(listGameInstance, seasonChart))
                                            .append("count", String.valueOf(seasonChart.getCountGame()))
                                            .append("countPostfix", Util.digitTranslate(seasonChart.getCountGame(), "игра", "игры", "игр"))
                                    ),
                                    null,
                                    null
                            ));

                    RegisterNotification.add(telegramNotifications);
                })
                .extension(NhlStatisticApplication::addOnError);
    }

    private static @NotNull String getString(List<NHLTeamSchedule.Game> listGameInstance, Chart.Response seasonChart) {
        String templateInGame = "🔹 Рекорд, скорее всего, не будет побит в этом сезоне";
        if (listGameInstance.size() >= seasonChart.getCountGame()) {
            NHLTeamSchedule.Game game = listGameInstance.get(seasonChart.getCountGame() - 1);
            templateInGame = String.format(
                    """
                            📅 Ключевой может стать игра 🆚 %s, %s (МСК)
                            """,
                    game.toggleTeam(UtilNHL.getOvi().getTeam()),
                    game.getMoscowDate()
            );
        }
        return templateInGame;
    }

}
