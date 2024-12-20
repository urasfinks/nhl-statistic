package ru.jamsys.core.handler.promise;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.flat.util.tank.UtilTank01;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.http.HttpResource;
import ru.jamsys.core.resource.http.client.HttpResponse;
import ru.jamsys.tank.data.NHLGamesForPlayer;
import ru.jamsys.tank.data.NHLPlayerList;
import ru.jamsys.tank.data.NHLTeamSchedule;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class GetPlayerScoreLastSeason implements PromiseGenerator {

    private String index;

    private String idTeam;

    private String idPlayer;

    private List<String> idGamesSeason = new ArrayList<>();

    public GetPlayerScoreLastSeason(String index, String idPlayer) {
        this.index = index;
        this.idPlayer = idPlayer;
    }

    @Override
    public Promise generate() {
        return App.get(ServicePromise.class).get(index, 6000L)
                //TODO: проверить может в БД уже есть стата по игроку и нам не надо ничего заапрашивать
                .extension(NHLPlayerList::promiseExtensionGetPlayerList)
                .then("getIdTeam", (_, _, promise) -> {
                    UtilTank01.Response response = promise.getRepositoryMapClass(UtilTank01.Response.class);
                    Map<String, Object> player = NHLPlayerList.findById(idPlayer, response.getData());
                    if (player == null || player.isEmpty()) {
                        throw new RuntimeException("Not found player");
                    }
                    idTeam = player.get("teamID").toString();
                })
                .extension(extendPromise -> UtilTank01.cacheRequest(
                        extendPromise,
                        _ -> NHLTeamSchedule.getUri(
                                idTeam,
                                NHLTeamSchedule.getCurrentSeasonIfRunOrNext() + ""
                        )
                ))
                .then("fillSeasonGame", (_, _, promise) -> {
                    UtilTank01.Response response = promise.getRepositoryMapClass(UtilTank01.Response.class);
                    NHLTeamSchedule.parseGameRaw(response.getData()).forEach(
                            map -> idGamesSeason.add(map.get("gameID").toString())
                    );
                })
                .thenWithResource(
                        "getBoxScore",
                        HttpResource.class,
                        (_, _, promise, httpResource) -> {
                            HttpResponse response = UtilTank01.request(
                                    httpResource,
                                    promise,
                                    _ -> NHLGamesForPlayer.getUri(idPlayer));
                            Map<String, Integer> onlyGoalsFilterSeason = NHLGamesForPlayer.getOnlyGoalsFilterSeason(
                                    response.getBody(),
                                    idGamesSeason
                            );
                            System.out.println(onlyGoalsFilterSeason);
                        })
                .setDebug(true)
                ;
    }

}
