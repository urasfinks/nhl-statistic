package ru.jamsys.core.handler.promise;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.NhlStatisticApplication;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.jt.JTTeamScheduler;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class InviteGameCommonTest {

    public static ServicePromise servicePromise;

    @BeforeAll
    static void beforeAll() {
        NhlStatisticApplication.startTelegramListener = false;
        App.getRunBuilder().addTestArguments().runCore();
        servicePromise = App.get(ServicePromise.class);
    }

    @AfterAll
    static void shutdown() {
        App.shutdown();
    }

    @Test
    void generate() {
        Promise promise = new InviteGameCommon().generate();
        PromiseTest promiseTest = new PromiseTest(promise);
        promiseTest.replace("select", promise.createTaskCompute("select", (_, _, promise1) -> {
            InviteGameCommon.Context context = promise1.getRepositoryMapClass(InviteGameCommon.Context.class);
            context.getListInviteGame().add(new JTTeamScheduler.RowInviteGame()
                    .setIdGame("20250129_PHI@NJ")
                    .setIdPlayer(new BigDecimal(3102))
                    .setIdChat(new BigDecimal(290029195L))
                    .setJson("""
                            {
                              "gameID" : "20250129_PHI@NJ",
                              "seasonType" : "Regular Season",
                              "away" : "PHI",
                              "gameTime" : "7:00p",
                              "teamIDHome" : "18",
                              "gameDate" : "20250129",
                              "gameStatus" : "Scheduled",
                              "gameTime_epoch" : "1738195200.0",
                              "teamIDAway" : "22",
                              "home" : "NJ",
                              "gameStatusCode" : "0",
                              "timeZone" : "-05:00",
                              "gameDateEpoch" : "20250130",
                              "gameDateTime" : "2025-01-29T19:00:00",
                              "gameDateTimeEpoch" : "2025-01-30T00:00:00",
                              "homeTeam" : "New Jersey Devils (NJ)",
                              "awayTeam" : "Philadelphia Flyers (PHI)",
                              "about" : "New Jersey Devils (NJ) vs Philadelphia Flyers (PHI)"
                            }""")
            );
        }));
        promiseTest.remove("check");
        promiseTest.remove("send");
        promiseTest.remove("update");
        Assertions.assertEquals("[select::WAIT, select::COMPUTE, handler::WAIT, handler::COMPUTE]", promiseTest.getIndex().toString());
        promise.setDebug(false).run().await(50_000L);
        InviteGameCommon.Context context = promise.getRepositoryMapClass(InviteGameCommon.Context.class);
        Assertions.assertEquals(1, context.getUniqueNotification().size());
        assertNull(context.getOviInviteGame());
        Assertions.assertEquals("[20250129_PHI@NJ]", context.getListIdGames().toString());
        Assertions.assertEquals(1, context.getListInviteGame().size());
    }

    @Test
    void generate2() {
        Promise promise = new InviteGameCommon().generate();
        PromiseTest promiseTest = new PromiseTest(promise);
        promiseTest.replace("select", promise.createTaskCompute("select", (_, _, promise1) -> {
            InviteGameCommon.Context context = promise1.getRepositoryMapClass(InviteGameCommon.Context.class);
            context.getListInviteGame().add(
                    new JTTeamScheduler.RowInviteGame()
                            .setIdGame("20250129_PHI@NJ")
                            .setIdPlayer(new BigDecimal(3102))
                            .setIdChat(new BigDecimal(290029195L))
                            .setJson("""
                                    {
                                      "gameID" : "20250129_PHI@NJ",
                                      "seasonType" : "Regular Season",
                                      "away" : "PHI",
                                      "gameTime" : "7:00p",
                                      "teamIDHome" : "18",
                                      "gameDate" : "20250129",
                                      "gameStatus" : "Scheduled",
                                      "gameTime_epoch" : "1738195200.0",
                                      "teamIDAway" : "22",
                                      "home" : "NJ",
                                      "gameStatusCode" : "0",
                                      "timeZone" : "-05:00",
                                      "gameDateEpoch" : "20250130",
                                      "gameDateTime" : "2025-01-29T19:00:00",
                                      "gameDateTimeEpoch" : "2025-01-30T00:00:00",
                                      "homeTeam" : "New Jersey Devils (NJ)",
                                      "awayTeam" : "Philadelphia Flyers (PHI)",
                                      "about" : "New Jersey Devils (NJ) vs Philadelphia Flyers (PHI)"
                                    }""")
            );
            context.getListInviteGame().add(
                    new JTTeamScheduler.RowInviteGame()
                            .setIdGame("20250129_PHI@NJ")
                            .setIdPlayer(new BigDecimal(3103))
                            .setIdChat(new BigDecimal(290029195L))
                            .setJson("""
                                    {
                                      "gameID" : "20250129_PHI@NJ",
                                      "seasonType" : "Regular Season",
                                      "away" : "PHI",
                                      "gameTime" : "7:00p",
                                      "teamIDHome" : "18",
                                      "gameDate" : "20250129",
                                      "gameStatus" : "Scheduled",
                                      "gameTime_epoch" : "1738195200.0",
                                      "teamIDAway" : "22",
                                      "home" : "NJ",
                                      "gameStatusCode" : "0",
                                      "timeZone" : "-05:00",
                                      "gameDateEpoch" : "20250130",
                                      "gameDateTime" : "2025-01-29T19:00:00",
                                      "gameDateTimeEpoch" : "2025-01-30T00:00:00",
                                      "homeTeam" : "New Jersey Devils (NJ)",
                                      "awayTeam" : "Philadelphia Flyers (PHI)",
                                      "about" : "New Jersey Devils (NJ) vs Philadelphia Flyers (PHI)"
                                    }""")
            );
        }));
        promiseTest.remove("check");
        promiseTest.remove("send");
        promiseTest.remove("update");
        Assertions.assertEquals("[select::WAIT, select::COMPUTE, handler::WAIT, handler::COMPUTE]", promiseTest.getIndex().toString());
        promise.setDebug(false).run().await(50_000L);
        InviteGameCommon.Context context = promise.getRepositoryMapClass(InviteGameCommon.Context.class);
        Assertions.assertEquals(1, context.getUniqueNotification().size());
        assertNull(context.getOviInviteGame());
        Assertions.assertEquals("[20250129_PHI@NJ]", context.getListIdGames().toString());
        Assertions.assertEquals(2, context.getListInviteGame().size());
    }

    @Test
    void generate3() {
        Promise promise = new InviteGameCommon().generate();
        PromiseTest promiseTest = new PromiseTest(promise);
        promiseTest.replace("select", promise.createTaskCompute("select", (_, _, promise1) -> {
            InviteGameCommon.Context context = promise1.getRepositoryMapClass(InviteGameCommon.Context.class);
            context.getListInviteGame().add(
                    new JTTeamScheduler.RowInviteGame()
                            .setIdGame("20250129_PHI@NJ")
                            .setIdPlayer(new BigDecimal(3102))
                            .setIdChat(new BigDecimal(290029195L))
                            .setJson("""
                                    {
                                      "gameID" : "20250129_PHI@NJ",
                                      "seasonType" : "Regular Season",
                                      "away" : "PHI",
                                      "gameTime" : "7:00p",
                                      "teamIDHome" : "18",
                                      "gameDate" : "20250129",
                                      "gameStatus" : "Scheduled",
                                      "gameTime_epoch" : "1738195200.0",
                                      "teamIDAway" : "22",
                                      "home" : "NJ",
                                      "gameStatusCode" : "0",
                                      "timeZone" : "-05:00",
                                      "gameDateEpoch" : "20250130",
                                      "gameDateTime" : "2025-01-29T19:00:00",
                                      "gameDateTimeEpoch" : "2025-01-30T00:00:00",
                                      "homeTeam" : "New Jersey Devils (NJ)",
                                      "awayTeam" : "Philadelphia Flyers (PHI)",
                                      "about" : "New Jersey Devils (NJ) vs Philadelphia Flyers (PHI)"
                                    }""")
            );
            context.getListInviteGame().add(
                    new JTTeamScheduler.RowInviteGame()
                            .setIdGame("20250129_PHI@NJ")
                            .setIdPlayer(new BigDecimal(3101))
                            .setIdChat(new BigDecimal(290029195L))
                            .setJson("""
                                    {
                                      "gameID" : "20250129_PHI@NJ",
                                      "seasonType" : "Regular Season",
                                      "away" : "PHI",
                                      "gameTime" : "7:00p",
                                      "teamIDHome" : "18",
                                      "gameDate" : "20250129",
                                      "gameStatus" : "Scheduled",
                                      "gameTime_epoch" : "1738195200.0",
                                      "teamIDAway" : "22",
                                      "home" : "NJ",
                                      "gameStatusCode" : "0",
                                      "timeZone" : "-05:00",
                                      "gameDateEpoch" : "20250130",
                                      "gameDateTime" : "2025-01-29T19:00:00",
                                      "gameDateTimeEpoch" : "2025-01-30T00:00:00",
                                      "homeTeam" : "New Jersey Devils (NJ)",
                                      "awayTeam" : "Philadelphia Flyers (PHI)",
                                      "about" : "New Jersey Devils (NJ) vs Philadelphia Flyers (PHI)"
                                    }""")
            );
        }));
        promiseTest.remove("check");
        promiseTest.remove("send");
        promiseTest.remove("update");
        Assertions.assertEquals("[select::WAIT, select::COMPUTE, handler::WAIT, handler::COMPUTE]", promiseTest.getIndex().toString());
        promise.setDebug(false).run().await(50_000L);
        InviteGameCommon.Context context = promise.getRepositoryMapClass(InviteGameCommon.Context.class);
        Assertions.assertEquals(1, context.getUniqueNotification().size());
        assertNotNull(context.getOviInviteGame());
        Assertions.assertEquals("[20250129_PHI@NJ]", context.getListIdGames().toString());
        Assertions.assertEquals(2, context.getListInviteGame().size());
    }

}