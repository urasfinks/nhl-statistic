package ru.jamsys.core.handler.promise;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.jt.JTGameDiff;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Заполняет listAlreadyDiffIdGame на основе существующих данных из таблицы GameDiff
// В GameDiff появляется игра, когда игра после старта игры появляется первичная статистика
// Это всё надо, чтобы из списка игр команды, вычислить будущие игры, за исключением начатых
// Список игр команды обновляется 1 раз в сутки, а это значит, что мы имеем не актуальный статус по играм
// Вот такой хитрый трюк, что бы работать с кешем игр команды и не испытывать проблем с поиском будущих игр

@Getter
@Setter
@Accessors(chain = true)
public class AlreadyDiffIdGame implements PromiseGenerator {

    final public List<String> listIdGames;

    public List<String> listAlreadyDiffIdGame = new ArrayList<>();

    public AlreadyDiffIdGame(List<String> listIdGames) {
        this.listIdGames = listIdGames;
    }

    @Override
    public Promise generate() {
        return App.get(ServicePromise.class).get(getClass().getSimpleName(), 60_000L)
                .thenWithResource(
                        "select",
                        JdbcResource.class,
                        (_, _, _, jdbcResource) -> {
                            List<Map<String, Object>> execute = jdbcResource.execute(new JdbcRequest(JTGameDiff.SELECT_ALREADY)
                                    .addArg("id_game", listIdGames)
                            );
                            execute.forEach(map -> listAlreadyDiffIdGame.add(map.get("id_game").toString()));
                        }
                );
    }


}
