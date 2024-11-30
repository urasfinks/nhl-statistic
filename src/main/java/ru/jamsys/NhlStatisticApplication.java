package ru.jamsys;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import ru.jamsys.core.App;

@SpringBootApplication
public class NhlStatisticApplication {

    public static void main(String[] args) {
        App.springSource = NhlStatisticApplication.class;
        App.main(args);
    }

    public static void addEventListener(String gameId, String player) { // Добавить слушатель игры

    }

    public static void getMyListeners(String chatId) { // Получить мои подписки

    }

    public static void getListCheckGameId() { // Получить список игр, для которых надо проверить изменения

    }

}
