package ru.jamsys;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import ru.jamsys.core.App;

@SpringBootApplication
public class NhlStatisticApplication {

    public static boolean startTelegramListener = false; // Запускать чтение сообщений от бота

    public static boolean dummySchedulerBoxScore = true; // Использовать заглушку получения информации о статусе игры

    public static void main(String[] args) {
        App.springSource = NhlStatisticApplication.class;
        App.main(args);
    }

}
