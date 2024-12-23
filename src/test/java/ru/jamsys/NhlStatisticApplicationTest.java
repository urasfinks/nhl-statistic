package ru.jamsys;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.resource.http.client.HttpResponse;
import ru.jamsys.core.resource.notification.telegram.TelegramNotificationRequest;
import ru.jamsys.core.resource.notification.telegram.TelegramNotificationResource;

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

    @Test
    void testGoalInSeason() {
        //new GetPlayerScoreCurrentSeason("xxkaa", "3101").generate().run().await(50_000L);
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
}