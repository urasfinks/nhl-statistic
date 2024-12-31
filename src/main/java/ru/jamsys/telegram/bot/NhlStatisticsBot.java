package ru.jamsys.telegram.bot;

import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.jamsys.core.component.manager.item.RouteGeneratorRepository;
import ru.jamsys.telegram.AbstractBot;

import java.util.ArrayList;
import java.util.List;

public class NhlStatisticsBot extends AbstractBot {

    public NhlStatisticsBot(String botUsername, String botToken, RouteGeneratorRepository routerRepository) throws TelegramApiException {
        super(botUsername, botToken, routerRepository);
        List<BotCommand> list = new ArrayList<>();
        list.add(new BotCommand("/subscribe_to_player", "Подписаться на игрока"));
        list.add(new BotCommand("/my_subscriptions", "Мои подписки"));
        list.add(new BotCommand("/remove_subscription", "Удалить подписку"));
        execute(new SetMyCommands(list, new BotCommandScopeDefault(), null));
    }

}
