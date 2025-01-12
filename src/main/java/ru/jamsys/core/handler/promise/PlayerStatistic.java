package ru.jamsys.core.handler.promise;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.builder.ArrayListBuilder;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.flat.template.twix.TemplateTwix;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilDate;
import ru.jamsys.core.flat.util.UtilListSort;
import ru.jamsys.core.flat.util.UtilNHL;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.promise.PromiseTask;
import ru.jamsys.tank.data.NHLGamesForPlayer;
import ru.jamsys.tank.data.NHLPlayerList;
import ru.jamsys.tank.data.NHLTeamSchedule;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@ToString
public class PlayerStatistic implements PromiseGenerator {

    private String date;

    private Map<String, Object> scoreToday;

    private Map<String, Object> scoreCurrentSeasons;

    private Map<String, Object> scoreTotal;

    private BigDecimal avgGoalsInGame = new BigDecimal(0);

    private Map<String, Object> nextGame = null;

    private final int scoreLastSeason;

    @JsonIgnore
    private final List<String> lisIdGameInSeason = new ArrayList<>();

    private int countTailGame = 0;

    private final NHLPlayerList.Player player;

    private String idGameToday = null;

    public PlayerStatistic(NHLPlayerList.Player player, int scoreLastSeason) {
        this.player = player;
        this.scoreLastSeason = scoreLastSeason;
    }

    public int getTotalGoals() {
        int seasonGoals = Integer.parseInt(scoreTotal.getOrDefault("goals", "0").toString());
        return scoreLastSeason + seasonGoals;
    }

    public String getMessage() {
        int seasonGoals = Integer.parseInt(scoreTotal.getOrDefault("goals", "0").toString());
        int countGame = Integer.parseInt(scoreTotal.getOrDefault("countGame", "").toString());

        int assists = Integer.parseInt(scoreTotal.getOrDefault("assists", "").toString());

        int totalGoals = scoreLastSeason + seasonGoals;
        int gretzkyOffset = UtilNHL.getScoreGretzky() - totalGoals;

        String templateNextGame = "";
        if (nextGame != null) {
            NHLTeamSchedule.Game game = new NHLTeamSchedule.Game(nextGame);
            templateNextGame = String.format(
                    "Следующая игра: 🆚 %s, %s",
                    game.toggleTeam(UtilNHL.getOvi().getTeam()),
                    game.getMoscowDate()
            );
        }

        return TemplateTwix.template("""
                Статистика Александра Овечкина на ${currentDate}:
                🎯 Забито голов: ${totalGoals}
                🏆 До рекорда Гретцки: ${gretzkyOffset} ${gretzkyOffsetPostfix}
                📅 Сезон ${seasonTitle}: ${countGame} ${countGamePostfix}, ${seasonGoals} ${seasonGoalsPostfix}, ${assists} ${assistsPostfix}, ${score} ${scorePostfix}, ${countTailGamePrefix} ${countTailGame} ${countTailGamePostfix} в регулярном чемпионате
                📈 Темп: В среднем ${avgGoalsInGame} гола за игру в этом сезоне
                
                ${templateNextGame}
                
                📍 Время указано по МСК
                """, new HashMapBuilder<String, String>()
                .append("currentDate", date)

                .append("totalGoals", String.valueOf(totalGoals))
                .append("totalGoalsPostfix", Util.digitTranslate(totalGoals, "гол", "гола", "голов"))

                .append("seasonGoals", String.valueOf(seasonGoals))
                .append("seasonGoalsPostfix", Util.digitTranslate(seasonGoals, "гол", "гола", "голов"))

                .append("gretzkyOffset", String.valueOf(gretzkyOffset))
                .append("gretzkyOffsetPostfix", Util.digitTranslate(gretzkyOffset, "гол", "гола", "голов"))

                .append("countTailGamePrefix", Util.digitTranslate(countTailGame, "остался", "осталось", "осталось"))
                .append("countTailGame", String.valueOf(countTailGame))
                .append("countTailGamePostfix", Util.digitTranslate(countTailGame, "матч", "матча", "матчей"))

                .append("avgGoalsInGame", avgGoalsInGame.toString())

                .append("countGame", String.valueOf(countGame))
                .append("countGamePostfix", Util.digitTranslate(countGame, "матч", "матча", "матчей"))

                .append("seasonTitle", UtilNHL.seasonFormat(UtilNHL.getActiveSeasonOrNext()))
                .append("assists", String.valueOf(assists))
                .append("assistsPostfix", Util.digitTranslate(assists, "передача", "передачи", "передач"))

                .append("score", String.valueOf(assists + seasonGoals))
                .append("scorePostfix", Util.digitTranslate(assists + seasonGoals, "очко", "очка", "очков"))

                .append("templateNextGame", templateNextGame)
        ).trim();
    }

    @Override
    public Promise generate() {
        return App.get(ServicePromise.class).get(getClass().getSimpleName(), 60_000L)
                .extension(promise -> promise.setRepositoryMapClass(PlayerStatistic.class, this)) // Просто для отладки
                .then("init", (_, _, promise) -> {
                    promise.getRepositoryMapClass(PlayerStatistic.class);
                    setDate(UtilDate.timestampFormatUTC(UtilDate.getTimestamp() + 3 * 60 * 60, "dd.MM.yyyy HH:mm"));
                })
                // На текущий момент мы не знаем конкретную игру, поэтому получаем всё
                .then("requestGameInSeason", new Tank01Request(() -> NHLTeamSchedule.getUri(
                        getPlayer().getTeamID(),
                        UtilNHL.getActiveSeasonOrNext() + ""
                )).generate())
                .then("parseGameInSeason", (_, _, promise) -> {
                    promise.getRepositoryMapClass(PlayerStatistic.class);
                    Tank01Request response = promise
                            .getRepositoryMapClass(Promise.class, "requestGameInSeason")
                            .getRepositoryMapClass(Tank01Request.class);

                    NHLTeamSchedule.Instance instance = new NHLTeamSchedule.Instance(response.getResponseData())
                            .initAlreadyGame();

                    List<Map<String, Object>> listGame = instance.getListGame();
                    listGame.forEach(map -> getLisIdGameInSeason().add(map.get("gameID").toString()));
                    if (getIdGameToday() == null && !listGame.isEmpty()) {
                        String gameToday = instance.getIdGameToday(UtilNHL.getCurrentDateEpoch());
                        if (gameToday != null && !gameToday.isEmpty()) {
                            setIdGameToday(gameToday);
                        }
                    }
                    setCountTailGame(instance
                            .getFutureGame()
                            .sort(UtilListSort.Type.ASC)
                            .getListGame()
                            .size()
                    );
                    setNextGame(instance
                            .getFutureGame()
                            .sort(UtilListSort.Type.ASC)
                            .getListGame()
                            .getFirst()
                    );

                    if (getIdGameToday() != null && !getIdGameToday().isEmpty()) {
                        promise.addToHead(new ArrayListBuilder<PromiseTask>()
                                .append(promise.promiseToTask(
                                        "scoreBoxCache",
                                        new ScoreBoxCache(getPlayer(), getIdGameToday()).generate()
                                ))
                                .append(promise.createTaskWait("scoreBoxCache"))
                                .append(promise.createTaskCompute(
                                        "parseScoreBoxCache",
                                        (_, _, p) -> {
                                            ScoreBoxCache scoreBoxCache = p
                                                    .getRepositoryMapClass(Promise.class, "scoreBoxCache")
                                                    .getRepositoryMapClass(ScoreBoxCache.class);
                                            setScoreToday(scoreBoxCache.getAllStatistic());
                                        }
                                ))
                        );
                        getLisIdGameInSeason().remove(getIdGameToday());
                    }
                })
                .then("requestGameByPlayer", new Tank01Request(() -> NHLGamesForPlayer.getUri(getPlayer().getPlayerID())).generate())
                .then("parseGameByPlayer", (_, _, promise) -> {
                    promise.getRepositoryMapClass(PlayerStatistic.class);
                    Tank01Request response = promise
                            .getRepositoryMapClass(Promise.class, "requestGameByPlayer")
                            .getRepositoryMapClass(Tank01Request.class);
                    setScoreCurrentSeasons(NHLGamesForPlayer.getAggregateStatistic(
                            response.getResponseData(),
                            getLisIdGameInSeason()
                    ));
                    List<Map<String, Object>> complex = new ArrayListBuilder<Map<String, Object>>()
                            .append(getScoreCurrentSeasons());

                    Map<String, Object> scoreToday = getScoreToday();
                    if (scoreToday != null && !scoreToday.isEmpty()) {
                        complex.add(scoreToday);
                    }
                    setScoreTotal(NHLGamesForPlayer.getAggregateStatistic(complex));
                    int playedGame = Integer.parseInt(getScoreTotal().getOrDefault("countGame", 0).toString());
                    int playedGoals = Integer.parseInt(getScoreTotal().getOrDefault("goals", 0).toString());
                    try {
                        setAvgGoalsInGame(new BigDecimal(playedGoals).divide(new BigDecimal(playedGame), 2, RoundingMode.HALF_UP));
                    } catch (Exception e) {
                        App.error(e);
                    }
                })
                .setDebug(false)
                ;
    }

}
