package ru.jamsys.core.component.cron;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.NhlStatisticApplication;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.component.TelegramBotComponent;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseTest;
import ru.jamsys.core.resource.jdbc.JdbcResource;
import ru.jamsys.tank.data.NHLBoxScore;
import ru.jamsys.telegram.GameEventData;

class MinSchedulerTest {

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

    @Test
    void test1() {
        Promise promise = new MinScheduler(
                App.get(ServicePromise.class),
                App.get(TelegramBotComponent.class),
                App.get(ServiceProperty.class)
        ).generate();
        PromiseTest promiseTest = new PromiseTest(promise);
        Assertions.assertEquals("[check::COMPUTE, getActiveGame::WAIT, getActiveGame::IO, getBoxScoreByActiveGame::WAIT, getBoxScoreByActiveGame::COMPUTE, getLastData::WAIT, getLastData::IO, getEvent::WAIT, getEvent::COMPUTE, selectSubscribers::WAIT, selectSubscribers::IO, getPlayerList::WAIT, getPlayerList::EXTERNAL_WAIT_COMPUTE, createNotification::WAIT, createNotification::COMPUTE, send::WAIT, send::COMPUTE, saveData::WAIT, saveData::IO, removeFinish::WAIT, removeFinish::IO]", promiseTest.getIndex().toString());
        promiseTest.remove("check");
        promiseTest.remove("getActiveGame");
        promiseTest.replace("getBoxScoreByActiveGame", promise.createTaskCompute("getBoxScoreByActiveGame", (_, _, promise1) -> {
            promise1.getRepositoryMapClass(MinScheduler.Context.class).getCurrentData().put("20241228_WSH@TOR", NHLBoxScore.getExampleTorWSH());
        }));
        promiseTest.replace("getLastData", promise.createTaskResource("getLastData", JdbcResource.class, (run, _, promise1, jdbcResource) -> {
            promise1.getRepositoryMapClass(MinScheduler.Context.class).getLastData().put("20241228_WSH@TOR", null);
        }));
        promiseTest.removeAfter("getEvent");
        Assertions.assertEquals("[getBoxScoreByActiveGame::WAIT, getBoxScoreByActiveGame::COMPUTE, getLastData::WAIT, getLastData::IO, getEvent::WAIT, getEvent::COMPUTE]", promiseTest.getIndex().toString());

        promise.then("saveData", (_, _, _) -> {
        });
        promise.setDebug(true).run().await(50_000L);

        Assertions.assertEquals(38, promise.getRepositoryMapClass(MinScheduler.Context.class).getMapIdPlayerGame().size());
        Assertions.assertEquals(2, promise.getRepositoryMapClass(MinScheduler.Context.class).getPlayerEvent().get("3101").size());
        Assertions.assertEquals(GameEventData.Action.START_GAME, promise.getRepositoryMapClass(MinScheduler.Context.class).getPlayerEvent().get("3101").getFirst().getAction());
        Assertions.assertEquals(GameEventData.Action.FINISH_GAME, promise.getRepositoryMapClass(MinScheduler.Context.class).getPlayerEvent().get("3101").getLast().getAction());
        Assertions.assertEquals("[20241228_WSH@TOR]", promise.getRepositoryMapClass(MinScheduler.Context.class).getEndGames().toString());
    }

    @Test
    void test2() {
        Promise promise = new MinScheduler(
                App.get(ServicePromise.class),
                App.get(TelegramBotComponent.class),
                App.get(ServiceProperty.class)
        ).generate();
        PromiseTest promiseTest = new PromiseTest(promise);
        Assertions.assertEquals("[check::COMPUTE, getActiveGame::WAIT, getActiveGame::IO, getBoxScoreByActiveGame::WAIT, getBoxScoreByActiveGame::COMPUTE, getLastData::WAIT, getLastData::IO, getEvent::WAIT, getEvent::COMPUTE, selectSubscribers::WAIT, selectSubscribers::IO, getPlayerList::WAIT, getPlayerList::EXTERNAL_WAIT_COMPUTE, createNotification::WAIT, createNotification::COMPUTE, send::WAIT, send::COMPUTE, saveData::WAIT, saveData::IO, removeFinish::WAIT, removeFinish::IO]", promiseTest.getIndex().toString());
        promiseTest.remove("check");
        promiseTest.remove("getActiveGame");
        promiseTest.replace("getBoxScoreByActiveGame", promise.createTaskCompute("getBoxScoreByActiveGame", (_, _, promise1) -> {
            promise1.getRepositoryMapClass(MinScheduler.Context.class).getCurrentData().put("20241228_WSH@TOR", NHLBoxScore.getExampleTorWSH());
        }));
        promiseTest.replace("getLastData", promise.createTaskResource("getLastData", JdbcResource.class, (run, _, promise1, jdbcResource) -> {
            promise1.getRepositoryMapClass(MinScheduler.Context.class).getLastData().put("20241228_WSH@TOR", NHLBoxScore.getExampleTorWSH());
        }));
        promiseTest.removeAfter("getEvent");
        Assertions.assertEquals("[getBoxScoreByActiveGame::WAIT, getBoxScoreByActiveGame::COMPUTE, getLastData::WAIT, getLastData::IO, getEvent::WAIT, getEvent::COMPUTE]", promiseTest.getIndex().toString());

        promise.then("saveData", (_, _, _) -> {
        });
        promise.setDebug(true).run().await(50_000L);

        Assertions.assertEquals(38, promise.getRepositoryMapClass(MinScheduler.Context.class).getMapIdPlayerGame().size());
        Assertions.assertEquals(1, promise.getRepositoryMapClass(MinScheduler.Context.class).getPlayerEvent().get("3101").size());
        Assertions.assertEquals(GameEventData.Action.FINISH_GAME, promise.getRepositoryMapClass(MinScheduler.Context.class).getPlayerEvent().get("3101").getLast().getAction());
        Assertions.assertEquals("[20241228_WSH@TOR]", promise.getRepositoryMapClass(MinScheduler.Context.class).getEndGames().toString());
    }

    @Test
    void test3() {
        Promise promise = new MinScheduler(
                App.get(ServicePromise.class),
                App.get(TelegramBotComponent.class),
                App.get(ServiceProperty.class)
        ).generate();
        PromiseTest promiseTest = new PromiseTest(promise);
        Assertions.assertEquals("[check::COMPUTE, getActiveGame::WAIT, getActiveGame::IO, getBoxScoreByActiveGame::WAIT, getBoxScoreByActiveGame::COMPUTE, getLastData::WAIT, getLastData::IO, getEvent::WAIT, getEvent::COMPUTE, selectSubscribers::WAIT, selectSubscribers::IO, getPlayerList::WAIT, getPlayerList::EXTERNAL_WAIT_COMPUTE, createNotification::WAIT, createNotification::COMPUTE, send::WAIT, send::COMPUTE, saveData::WAIT, saveData::IO, removeFinish::WAIT, removeFinish::IO]", promiseTest.getIndex().toString());
        promiseTest.remove("check");
        promiseTest.remove("getActiveGame");
        promiseTest.replace("getBoxScoreByActiveGame", promise.createTaskCompute("getBoxScoreByActiveGame", (_, _, promise1) -> {
            promise1.getRepositoryMapClass(MinScheduler.Context.class).getCurrentData().put("20241228_WSH@TOR", NHLBoxScore.getExampleTorWSH_live());
        }));
        promiseTest.replace("getLastData", promise.createTaskResource("getLastData", JdbcResource.class, (run, _, promise1, jdbcResource) -> {
            promise1.getRepositoryMapClass(MinScheduler.Context.class).getLastData().put("20241228_WSH@TOR", NHLBoxScore.getExampleTorWSH());
        }));
        promiseTest.removeAfter("getEvent");
        Assertions.assertEquals("[getBoxScoreByActiveGame::WAIT, getBoxScoreByActiveGame::COMPUTE, getLastData::WAIT, getLastData::IO, getEvent::WAIT, getEvent::COMPUTE]", promiseTest.getIndex().toString());

        promise.then("saveData", (_, _, _) -> {
        });
        promise.setDebug(true).run().await(50_000L);

        Assertions.assertEquals(0, promise.getRepositoryMapClass(MinScheduler.Context.class).getMapIdPlayerGame().size());
        Assertions.assertEquals("[]", promise.getRepositoryMapClass(MinScheduler.Context.class).getEndGames().toString());
    }

    @Test
    void test4() {
        Promise promise = new MinScheduler(
                App.get(ServicePromise.class),
                App.get(TelegramBotComponent.class),
                App.get(ServiceProperty.class)
        ).generate();
        PromiseTest promiseTest = new PromiseTest(promise);
        Assertions.assertEquals("[check::COMPUTE, getActiveGame::WAIT, getActiveGame::IO, getBoxScoreByActiveGame::WAIT, getBoxScoreByActiveGame::COMPUTE, getLastData::WAIT, getLastData::IO, getEvent::WAIT, getEvent::COMPUTE, selectSubscribers::WAIT, selectSubscribers::IO, getPlayerList::WAIT, getPlayerList::EXTERNAL_WAIT_COMPUTE, createNotification::WAIT, createNotification::COMPUTE, send::WAIT, send::COMPUTE, saveData::WAIT, saveData::IO, removeFinish::WAIT, removeFinish::IO]", promiseTest.getIndex().toString());
        promiseTest.remove("check");
        promiseTest.remove("getActiveGame");
        promiseTest.replace("getBoxScoreByActiveGame", promise.createTaskCompute("getBoxScoreByActiveGame", (_, _, promise1) -> {
            promise1.getRepositoryMapClass(MinScheduler.Context.class).getCurrentData().put("20241228_WSH@TOR", NHLBoxScore.getExampleTorWSH_live());
        }));
        promiseTest.replace("getLastData", promise.createTaskResource("getLastData", JdbcResource.class, (run, _, promise1, jdbcResource) -> {
            promise1.getRepositoryMapClass(MinScheduler.Context.class).getLastData().put("20241228_WSH@TOR", null);
        }));
        promiseTest.removeAfter("getEvent");
        Assertions.assertEquals("[getBoxScoreByActiveGame::WAIT, getBoxScoreByActiveGame::COMPUTE, getLastData::WAIT, getLastData::IO, getEvent::WAIT, getEvent::COMPUTE]", promiseTest.getIndex().toString());

        promise.then("saveData", (_, _, _) -> {
        });
        promise.setDebug(true).run().await(50_000L);

        Assertions.assertEquals(38, promise.getRepositoryMapClass(MinScheduler.Context.class).getMapIdPlayerGame().size());
        Assertions.assertEquals(1, promise.getRepositoryMapClass(MinScheduler.Context.class).getPlayerEvent().get("3101").size());
        Assertions.assertEquals(GameEventData.Action.START_GAME, promise.getRepositoryMapClass(MinScheduler.Context.class).getPlayerEvent().get("3101").getFirst().getAction());
        Assertions.assertEquals("[]", promise.getRepositoryMapClass(MinScheduler.Context.class).getEndGames().toString());
    }

    @Test
    void test5() {
        Promise promise = new MinScheduler(
                App.get(ServicePromise.class),
                App.get(TelegramBotComponent.class),
                App.get(ServiceProperty.class)
        ).generate();
        PromiseTest promiseTest = new PromiseTest(promise);
        Assertions.assertEquals("[check::COMPUTE, getActiveGame::WAIT, getActiveGame::IO, getBoxScoreByActiveGame::WAIT, getBoxScoreByActiveGame::COMPUTE, getLastData::WAIT, getLastData::IO, getEvent::WAIT, getEvent::COMPUTE, selectSubscribers::WAIT, selectSubscribers::IO, getPlayerList::WAIT, getPlayerList::EXTERNAL_WAIT_COMPUTE, createNotification::WAIT, createNotification::COMPUTE, send::WAIT, send::COMPUTE, saveData::WAIT, saveData::IO, removeFinish::WAIT, removeFinish::IO]", promiseTest.getIndex().toString());
        promiseTest.remove("check");
        promiseTest.remove("getActiveGame");
        promiseTest.replace("getBoxScoreByActiveGame", promise.createTaskCompute("getBoxScoreByActiveGame", (_, _, promise1) -> {
            promise1.getRepositoryMapClass(MinScheduler.Context.class).getCurrentData().put("20241228_WSH@TOR", NHLBoxScore.getExampleTorWSH_liveChange());
        }));
        promiseTest.replace("getLastData", promise.createTaskResource("getLastData", JdbcResource.class, (run, _, promise1, jdbcResource) -> {
            promise1.getRepositoryMapClass(MinScheduler.Context.class).getLastData().put("20241228_WSH@TOR", NHLBoxScore.getExampleTorWSH_live());
        }));
        promiseTest.removeAfter("getEvent");
        Assertions.assertEquals("[getBoxScoreByActiveGame::WAIT, getBoxScoreByActiveGame::COMPUTE, getLastData::WAIT, getLastData::IO, getEvent::WAIT, getEvent::COMPUTE]", promiseTest.getIndex().toString());

        promise.then("saveData", (_, _, _) -> {
        });
        promise.setDebug(true).run().await(50_000L);

        Assertions.assertEquals(1, promise.getRepositoryMapClass(MinScheduler.Context.class).getMapIdPlayerGame().size());
        Assertions.assertEquals(1, promise.getRepositoryMapClass(MinScheduler.Context.class).getPlayerEvent().get("3101").size());
        Assertions.assertEquals(GameEventData.Action.GOAL, promise.getRepositoryMapClass(MinScheduler.Context.class).getPlayerEvent().get("3101").getFirst().getAction());
        Assertions.assertEquals("[]", promise.getRepositoryMapClass(MinScheduler.Context.class).getEndGames().toString());
    }

    @Test
    void test6() {
        Promise promise = new MinScheduler(
                App.get(ServicePromise.class),
                App.get(TelegramBotComponent.class),
                App.get(ServiceProperty.class)
        ).generate();
        PromiseTest promiseTest = new PromiseTest(promise);
        Assertions.assertEquals("[check::COMPUTE, getActiveGame::WAIT, getActiveGame::IO, getBoxScoreByActiveGame::WAIT, getBoxScoreByActiveGame::COMPUTE, getLastData::WAIT, getLastData::IO, getEvent::WAIT, getEvent::COMPUTE, selectSubscribers::WAIT, selectSubscribers::IO, getPlayerList::WAIT, getPlayerList::EXTERNAL_WAIT_COMPUTE, createNotification::WAIT, createNotification::COMPUTE, send::WAIT, send::COMPUTE, saveData::WAIT, saveData::IO, removeFinish::WAIT, removeFinish::IO]", promiseTest.getIndex().toString());
        promiseTest.remove("check");
        promiseTest.remove("getActiveGame");
        promiseTest.replace("getBoxScoreByActiveGame", promise.createTaskCompute("getBoxScoreByActiveGame", (_, _, promise1) -> {
            promise1.getRepositoryMapClass(MinScheduler.Context.class).getCurrentData().put("20241228_WSH@TOR", NHLBoxScore.getExampleTorWSH_liveChange());
        }));
        promiseTest.replace("getLastData", promise.createTaskResource("getLastData", JdbcResource.class, (run, _, promise1, jdbcResource) -> {
            promise1.getRepositoryMapClass(MinScheduler.Context.class).getLastData().put("20241228_WSH@TOR", NHLBoxScore.getExampleTorWSH_live());
        }));
        promiseTest.removeAfter("createNotification");
        Assertions.assertEquals("[getBoxScoreByActiveGame::WAIT, getBoxScoreByActiveGame::COMPUTE, getLastData::WAIT, getLastData::IO, getEvent::WAIT, getEvent::COMPUTE, selectSubscribers::WAIT, selectSubscribers::IO, getPlayerList::WAIT, getPlayerList::EXTERNAL_WAIT_COMPUTE, createNotification::WAIT, createNotification::COMPUTE]", promiseTest.getIndex().toString());

        promise.then("saveData", (_, _, _) -> {
        });
        promise.setDebug(true).run().await(50_000L);

        Assertions.assertEquals(1, promise.getRepositoryMapClass(MinScheduler.Context.class).getMapIdPlayerGame().size());
        Assertions.assertEquals(1, promise.getRepositoryMapClass(MinScheduler.Context.class).getPlayerEvent().get("3101").size());
        Assertions.assertEquals(GameEventData.Action.GOAL, promise.getRepositoryMapClass(MinScheduler.Context.class).getPlayerEvent().get("3101").getFirst().getAction());
        Assertions.assertEquals("[]", promise.getRepositoryMapClass(MinScheduler.Context.class).getEndGames().toString());
        Assertions.assertFalse(promise.getRepositoryMapClass(MinScheduler.Context.class).getPlayerSubscriber().get("3101").isEmpty());
        Assertions.assertEquals(2, promise.getRepositoryMapClass(MinScheduler.Context.class).getNotificationList().size());
    }

}