package ru.jamsys.core.component.cron;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.UniqueClassName;
import ru.jamsys.core.flat.template.cron.release.Cron3s;
import ru.jamsys.core.flat.util.UtilRisc;
import ru.jamsys.core.jt.JTGameDiff;
import ru.jamsys.core.jt.JTScheduler;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.http.HttpResource;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;
import ru.jamsys.tank.data.NHLBoxScore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Lazy
public class MinScheduler implements Cron3s, PromiseGenerator, UniqueClassName {

    private final ServicePromise servicePromise;

    @Setter
    @Getter
    private String index;

    public MinScheduler(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Setter
    @Getter
    public static class Context {
        List<String> listIdGame = new ArrayList<>();
        Map<String, String> response = new HashMap<>();
    }

    @Override
    public Promise generate() {
        return servicePromise.get(index, 6_000L)
                .thenWithResource("getSubscriptionsPlayer", JdbcResource.class, (_, _, promise, jdbcResource) -> {
                    Context context = promise.setRepositoryMapClass(Context.class, new Context());
                    List<Map<String, Object>> execute = jdbcResource.execute(new JdbcRequest(JTScheduler.SELECT_ACTIVE_GAME)
                            .setDebug(false)
                    );
                    if (execute.isEmpty()) {
                        promise.skipAllStep();
                        return;
                    }
                    List<String> listIdGame = context.getListIdGame();
                    execute.forEach(stringObjectMap -> listIdGame.add(stringObjectMap.get("id_game").toString()));
                })
                .thenWithResource("request", HttpResource.class, (run, _, promise, httpResource) -> {
                    Context context = promise.getRepositoryMapClass(Context.class);
                    List<String> listIdGame = context.getListIdGame();
                    for (String idGame : listIdGame) {
                        if (!run.get()) {
                            return;
                        }
//                        HttpResponse response = UtilTank01.request(httpResource, promise, _ -> NHLBoxScore.getUri(idGame));
//                        context.getResponse().put(idGame, response.getBody());
                        context.getResponse().put(idGame, NHLBoxScore.getExample());
                    }
                    if (context.getResponse().isEmpty()) {
                        promise.skipAllStep();
                    }
                })
                .thenWithResource("parseResponse", JdbcResource.class, (run, _, promise, jdbcResource) -> {
                    Context context = promise.getRepositoryMapClass(Context.class);
                    UtilRisc.forEach(run, context.getResponse(), (key, data) -> {
                        try {
                            List<Map<String, Object>> execute = jdbcResource.execute(new JdbcRequest(JTGameDiff.SELECT)
                                    .addArg("id_game", key)
                            );
                            jdbcResource.execute(new JdbcRequest(execute.isEmpty() ? JTGameDiff.INSERT : JTGameDiff.UPDATE)
                                    .addArg("id_game", key)
                                    .addArg("scoring_plays", data)
                            );
                        } catch (Throwable e) {
                            App.error(e);
                        }
                        System.out.println(key);
                        System.out.println(data);
                    });
                })
                ;
    }

}
