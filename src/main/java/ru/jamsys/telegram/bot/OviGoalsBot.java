package ru.jamsys.telegram.bot;

import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.jamsys.core.component.manager.item.RouteGeneratorRepository;
import ru.jamsys.telegram.AbstractBot;

import java.util.ArrayList;
import java.util.List;

public class OviGoalsBot extends AbstractBot {

    public OviGoalsBot(String botUsername, String botToken, RouteGeneratorRepository routerRepository) throws TelegramApiException {
        super(botUsername, botToken, routerRepository);
        List<BotCommand> list = new ArrayList<>();
        list.add(new BotCommand("/start", "Включить уведомления"));
        list.add(new BotCommand("/stats", "Текущая статистика: количество голов, оставшихся до рекорда, и статистика по сезону"));
        list.add(new BotCommand("/vote", "Статистика голосования"));
        list.add(new BotCommand("/schedule", "Ближайшие игры Александра Овечкина и команды Washington Capitals"));
        list.add(new BotCommand("/stop", "Отключить уведомления"));
        execute(new SetMyCommands(list, new BotCommandScopeDefault(), null));
    }

}
