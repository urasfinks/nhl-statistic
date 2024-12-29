package ru.jamsys;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.builder.ArrayListBuilder;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.handler.promise.*;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.resource.http.client.HttpResponse;
import ru.jamsys.core.resource.notification.telegram.TelegramNotificationRequest;
import ru.jamsys.core.resource.notification.telegram.TelegramNotificationResource;
import ru.jamsys.tank.data.NHLBoxScore;
import ru.jamsys.tank.data.NHLPlayerList;
import ru.jamsys.telegram.NotificationDataAndTemplate;

class NhlStatisticApplicationTest {

    public static ServicePromise servicePromise;

    @BeforeAll
    static void beforeAll() {
        App.getRunBuilder().addTestArguments().runCore();
        servicePromise = App.get(ServicePromise.class);
        NhlStatisticApplication.startTelegramListener = false;
        NhlStatisticApplication.dummySchedulerBoxScore = true;
    }

    @AfterAll
    static void shutdown() {
        App.shutdown();
    }

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

    //@Test
    void sendNotification() {
        String idGame = "20241008_CHI@UTA";
        NHLPlayerList.Player player = new NHLPlayerList.Player()
                .setPlayerID("4874723")
                .setPos("RW")
                .setLongName("Dylan Guenther")
                .setTeam("UTA")
                .setTeamID("33");
        NotificationDataAndTemplate notificationDataAndTemplate = new NotificationDataAndTemplate()
                .setAction("GOAL")
                .setScoredTitle("goal")
                .setScoredGoal(1)
                .setScoredEnum(new ArrayListBuilder<String>().append("any enum period"));
        new SendNotification(
                idGame,
                player,
                notificationDataAndTemplate,
                new ArrayListBuilder<Integer>().append(290029195)
        ).generate().run().await(50_000L);
    }

    @SuppressWarnings("unused")
        //@Test
    void telegramSend() {
        Promise promise = servicePromise.get("testPromise", 6_000L);
        promise
                .appendWithResource("http", TelegramNotificationResource.class, (_, _, _, telegramNotificationResource) -> {
                    HttpResponse execute = telegramNotificationResource.execute(new TelegramNotificationRequest("Hello", "world"));
                    System.out.println(execute);
                })
                .run()
                .await(2000);
        System.out.println(promise.getLogString());
    }

    //@Test
    void testRequest() {
        Tank01Request tank01Request = new Tank01Request(() -> NHLBoxScore.getUri("20241129_NYR@PHI"));
        tank01Request.setOnlyCache(true);
        tank01Request.generate()
                .run()
                .await(60_000L, 200);
        System.out.println(tank01Request.getResponseData());
    }

    //@Test
    void testScoreCache() {
        NHLPlayerList.Player player = new NHLPlayerList.Player()
                .setPlayerID("4565257")
                .setPos("RW")
                .setLongName("Bobby Brink")
                .setTeam("PHI")
                .setTeamID("22");
        ScoreBoxCache scoreBoxCache = new ScoreBoxCache(player, "20241129_NYR@PHI");

        scoreBoxCache.generate()
                .run()
                .await(60_000L);
        System.out.println(scoreBoxCache.getGoals());
        System.out.println(scoreBoxCache.getAllStatistic());
    }

    //@Test
    void testPlayerStat() {
        NHLPlayerList.Player player = new NHLPlayerList.Player()
                .setPlayerID("4565257")
                .setPos("RW")
                .setLongName("Bobby Brink")
                .setTeam("PHI")
                .setTeamID("22");
        PlayerStatistic playerStatistic = new PlayerStatistic(player, 999);
        playerStatistic.setGameToday("20241129_NYR@PHI");

        playerStatistic.generate()
                .run()
                .await(60_000L);
        System.out.println(UtilJson.toStringPretty(playerStatistic, "{}"));
    }

    @Test
    void testPlayerOvi() {
        PlayerStatistic playerStatistic = new PlayerStatisticOvi();
        playerStatistic.generate()
                .run()
                .await(60_000L);
        System.out.println(UtilJson.toStringPretty(playerStatistic, "{}"));
        System.out.println(playerStatistic.getMessage());
    }

}