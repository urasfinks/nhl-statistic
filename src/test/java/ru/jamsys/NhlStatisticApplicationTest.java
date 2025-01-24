package ru.jamsys;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.TelegramBotManager;
import ru.jamsys.core.extension.builder.ArrayListBuilder;
import ru.jamsys.core.flat.util.*;
import ru.jamsys.core.handler.promise.*;
import ru.jamsys.core.jt.JTLogRequest;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;
import ru.jamsys.tank.data.NHLBoxScore;
import ru.jamsys.tank.data.NHLGamesForPlayer;
import ru.jamsys.tank.data.NHLPlayerList;
import ru.jamsys.telegram.GameEventData;
import ru.jamsys.telegram.TelegramNotification;

import java.util.Map;
import java.util.Objects;

class NhlStatisticApplicationTest {

    public static ServicePromise servicePromise;

    @BeforeAll
    static void beforeAll() {
        App.getRunBuilder().addTestArguments().runCore();
        servicePromise = App.get(ServicePromise.class);
        NhlStatisticApplication.startTelegramListener = false;
    }

    @AfterAll
    static void shutdown() {
        App.shutdown();
    }

    @SuppressWarnings("unused")
        //@Test
    void getPlayerScoreCurrentSeason() {
        NHLPlayerList.Player player = new NHLPlayerList.Player()
                .setPlayerID("4874723")
                .setPos("RW")
                .setLongName("Dylan Guenther")
                .setTeam("UTA")
                .setTeamID("33");
        new ScorePlayerCurrentSeasonBeforeGame(player, "20241008_CHI@UTA")
                .generate()
                .run()
                .await(50_000L);
    }

    @SuppressWarnings("unused")
        //@Test
    void sendNotification() {
        String idGame = "20241008_CHI@UTA";
        NHLPlayerList.Player player = new NHLPlayerList.Player()
                .setPlayerID("4874723")
                .setPos("RW")
                .setLongName("Dylan Guenther")
                .setTeam("UTA")
                .setTeamID("33");
        GameEventData gameEventData = new GameEventData(
                GameEventData.Action.GOAL,
                "Washington Capitals (WSH) 🆚 Detroit Red Wings (DET)",
                "Washington Capitals (WSH) 1 - 1 Detroit Red Wings (DET)",
                player,
                UtilDate.get("dd.MM.yyyy HH:mm:ss")
        )
                .setScoredGoal(1)
                .setScoredLastSeason(300);
        new RegisterNotificationGameEvent(
                idGame,
                player,
                gameEventData,
                new ArrayListBuilder<Long>().append(290029195L)
        ).generate().run().await(50_000L);
    }

    @SuppressWarnings("unused")
        //@Test
    void telegramSend() {
        Promise promise = servicePromise.get("testPromise", 6_000L);
        promise
                .thenWithResource("insert", JdbcResource.class, (_, _, _, jdbcResource) -> jdbcResource.execute(
                        new JdbcRequest(JTLogRequest.INSERT)
                                .addArg("url", "url")
                                .addArg("data", "data")
                                .setDebug(false)
                ))
//                .appendWithResource("http", TelegramNotificationResource.class, (_, _, _, telegramNotificationResource) -> {
//                    HttpResponse execute = telegramNotificationResource.execute(new TelegramNotificationRequest("Hello", "world"));
//                })
                .run()
                .await(20_000L);
        Util.logConsole(getClass(), promise.getLogString());
    }

    @SuppressWarnings("unused")
        //@Test
    void testRequest() {
        Tank01Request tank01Request = new Tank01Request(() -> NHLBoxScore.getUri("20241129_NYR@PHI"));
        tank01Request.setOnlyCache(true);
        tank01Request.generate()
                .run()
                .await(60_000L, 200);
        Util.logConsole(getClass(), tank01Request.getResponseData());
    }

    @SuppressWarnings("unused")
        //@Test
    void testScoreCache() {
        NHLPlayerList.Player player = new NHLPlayerList.Player()
                .setPlayerID("4565257")
                .setPos("RW")
                .setLongName("Bobby Brink")
                .setTeam("PHI")
                .setTeamID("22");
        ScoreBoxCache scoreBoxCache = new ScoreBoxCache(player, "20241129_NYR@PHI2");

        scoreBoxCache.generate()
                .run()
                .await(60_000L);
        Util.logConsoleJson(getClass(), scoreBoxCache.getGoals());
        Util.logConsoleJson(getClass(), scoreBoxCache.getAllStatistic());
    }

    @SuppressWarnings("unused")
        //@Test
    void testPlayerStat() {
        NHLPlayerList.Player player = new NHLPlayerList.Player()
                .setPlayerID("4565257")
                .setPos("RW")
                .setLongName("Bobby Brink")
                .setTeam("PHI")
                .setTeamID("22");
        PlayerStatistic playerStatistic = new PlayerStatistic(player, 999);
        playerStatistic.setIdGameToday("20241129_NYR@PHI");

        playerStatistic.generate()
                .run()
                .await(60_000L);
        Util.logConsoleJson(getClass(), playerStatistic);
    }

    @SuppressWarnings("unused")
        //@Test
    void testPlayerOvi() {
        PlayerStatistic playerStatistic = new PlayerStatistic(UtilNHL.getOvi(), UtilNHL.getOviScoreLastSeason());
        playerStatistic.generate()
                .run()
                .await(60_000L);
        Util.logConsoleJson(getClass(), playerStatistic);
        Util.logConsole(getClass(), playerStatistic.getMessage());
    }

    @SuppressWarnings("unused")
        //@Test
    void httpCacheReset() {
        NHLPlayerList.Player player = UtilNHL.getOvi();
        new HttpCacheReset(NHLGamesForPlayer.getUri(player.getPlayerID())).generate().run().await(50_000L);
    }

    @SuppressWarnings("unused")
        //@Test
    void sendOvi() {
        String idGame = "20241008_CHI@UTA";
        NHLPlayerList.Player player = UtilNHL.getOvi();
        GameEventData gameEventData = new GameEventData(
                GameEventData.Action.START_GAME,
                "Washington Capitals (WSH) 🆚 Detroit Red Wings (DET)",
                "Washington Capitals (WSH) 1 - 1 Detroit Red Wings (DET)",
                player,
                UtilDate.get("dd.MM.yyyy HH:mm:ss")
        )
                .setScoredGoal(1)
                .setScoredLastSeason(300);
        new RegisterNotificationGameEventOvi(
                idGame,
                gameEventData
        ).generate().run().await(50_000L);
    }

    @SuppressWarnings("unused")
        //@Test
    void save() {
        Promise promise = servicePromise.get("testPromise", 600_000L);
        promise
                .thenWithResource(
                        "insert",
                        JdbcResource.class,
                        (_, _, _, jdbcResource) -> jdbcResource
                                .execute(new JdbcRequest(JTLogRequest.SELECT_NHL_BOX_SCORE)).forEach(map -> {
                                    try {
                                        Map<String, Object> mapOrThrow = UtilJson
                                                .getMapOrThrow(map.get("data").toString());
                                        UtilFile.writeBytes(
                                                "block4/" + map.get("id") + ".json",
                                                Objects.requireNonNull(UtilJson.toStringPretty(mapOrThrow, "{}"))
                                                        .getBytes(),
                                                FileWriteOptions.CREATE_OR_REPLACE);
                                    } catch (Throwable e) {
                                        App.error(e);
                                    }
                                }))
                .run()
                .await(20_000L);
        Util.logConsole(getClass(), promise.getLogString());
    }

    @SuppressWarnings("unused")
        //@Test
    void unsubscribe() {
        new RemoveSubscriberOvi(290029195L).generate().run().await(5_000L);
    }

    @SuppressWarnings("unused")
        //@Test
    void updateScheduler() {
        new UpdateScheduler(false).generate().run().await(50_000L);
    }

    //@Test
    void sendTelegram() {
//        TelegramNotification telegramNotification = new TelegramNotification(
//                290029195L,
//                App.get(TelegramBotManager.class).getOviBotProperty().getName(),
//                """
//                        Матч Pittsburgh Penguins (PIT) 🆚 Washington Capitals (WSH) начнется уже через 12 часов — 19 января в 03:00 (МСК).
//
//                        Как думаешь, сможет ли Александр Овечкин забить сегодня?
//
//                        """,
//                new ArrayListBuilder<Button>()
//                        .append(new Button(
//                                "Да 🔥",
//                                ServletResponseWriter.buildUrlQuery(
//                                        "/poll_quest/",
//                                        new HashMapBuilder<String, String>()
//                                                .append("value", "true")
//
//                                )
//                        ))
//                        .append(new Button(
//                                "Нет ⛔",
//                                ServletResponseWriter.buildUrlQuery(
//                                        "/poll_quest/",
//                                        new HashMapBuilder<String, String>()
//                                                .append("value", "false")
//
//                                )
//                        ))
//                ,
//                null
//        );
        TelegramNotification telegramNotification = new TelegramNotification(
                290029195L,
                App.get(TelegramBotManager.class).getOviBotProperty().getName(),
                null,
                null,
                "image/873.png"
        );
        UtilTelegramResponse.Result send = App.get(TelegramBotManager.class).send(telegramNotification, TelegramBotManager.TypeSender.HTTP);
        Util.logConsoleJson(NhlStatisticApplication.class, send);
    }

    //@Test
    void testInviteGame() {
        new InviteGameCommon().generate().run().await(60_000L);
    }

    //@Test
    void testGoals() {
        ScorePlayerCurrentSeasonBeforeGame stat = new ScorePlayerCurrentSeasonBeforeGame(NHLPlayerList.findByIdStatic(UtilNHL.getOvi().getPlayerID()), "20250123_WSH@SEA");
        stat
                .generate()
                .run()
                .await(50_000L);
        System.out.println(stat.getCountGoal().get());

    }

}