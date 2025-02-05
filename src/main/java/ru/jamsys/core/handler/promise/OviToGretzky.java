package ru.jamsys.core.handler.promise;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.flat.util.*;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.tank.data.NHLGamesForPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
@Setter
@ToString
public class OviToGretzky implements PromiseGenerator {

    public static class Context {

    }

    @Override
    public Promise generate() {
        return App.get(ServicePromise.class).get(getClass().getSimpleName(), 60_000L)
                .extension(promise -> promise.setRepositoryMapClass(Context.class, new Context()))
                .then("requestGamesForPlayer", new Tank01Request(() -> NHLGamesForPlayer.getUri(
                        UtilNHL.getOvi().getPlayerID()
                )).generate())
                .then("init", (_, _, promise) -> {
                    Tank01Request tank01Request = promise
                            .getRepositoryMapClass(Promise.class, "requestGamesForPlayer")
                            .getRepositoryMapClass(Tank01Request.class);
                    Map<String, Integer> onlyGoalsFilter = NHLGamesForPlayer
                            .getOnlyGoalsFilter(tank01Request.getResponseData(), null);
                    Util.logConsoleJson(getClass(), onlyGoalsFilter);
                    List<Double> list = new ArrayList<>();
                    AtomicInteger c = new AtomicInteger(0);
                    AtomicInteger ix = new AtomicInteger(0);
                    AtomicInteger counter = new AtomicInteger(0);
                    UtilRisc.forEach(null, onlyGoalsFilter.keySet(), idGame -> {
                        int i2 = counter.incrementAndGet();
                        if (i2 < 90) {
                            return;
                        }
                        Integer i = onlyGoalsFilter.get(idGame);
                        int i1 = c.addAndGet(i);
                        list.add((double) i1);
                        System.out.println(ix.incrementAndGet() + "," + i1);
                    }, true);
                    //System.out.println(list.size());
                    System.out.println(UtilJson.toString(list, "[]"));
                    //Util.logConsoleJson(getClass(), list);
                    double[] array = list.stream().mapToDouble(Double::doubleValue).toArray();

                    System.out.println("Linear: " + getOffsetLinear(2, array));
                    System.out.println("Poly2: " + getOffset(2, array));
                    System.out.println("Poly3: " + getOffset(3, array));
                    System.out.println("Poly4: " + getOffset(4, array));
                    System.out.println("Poly5: " + getOffset(5, array));
                    System.out.println("Poly6: " + getOffset(6, array));
                    System.out.println("Poly7: " + getOffset(7, array));
                    System.out.println("Poly8: " + getOffset(8, array));
                    System.out.println("Poly9: " + getOffset(9, array));
                })
                .setDebug(false)
                ;
    }

    public static int getOffset(int degree, double[] array) {
        int need = 72;
        int cc = 1;
        int maxCc = 100;
        while (true) {
            long round = Math.round(UtilTrend.getPoly(degree, array, cc));
            cc++;
            if (round >= need) {
                break;
            }
            maxCc--;
            if (maxCc <= 0) {
                break;
            }
        }
        return cc;
    }

    public static int getOffsetLinear(int degree, double[] array) {
        int need = 72;
        int cc = 1;
        int maxCc = 100;
        while (true) {
            long round = Math.round(UtilTrend.getLinear(array, cc));
            cc++;
            if (round >= need) {
                break;
            }
            maxCc--;
            if (maxCc <= 0) {
                break;
            }
        }
        return cc;
    }

}
