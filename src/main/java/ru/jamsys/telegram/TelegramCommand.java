package ru.jamsys.telegram;

import lombok.Getter;
import ru.jamsys.core.extension.functional.FunctionThrowing;
import ru.jamsys.telegram.command.SubscribeToPlayer;
import ru.jamsys.telegram.command.TelegramContext;

public enum TelegramCommand {

    SUBSCRIBE_TO_PLAYER("/subscribe_to_player", idChat -> new SubscribeToPlayer().setIdChat(idChat));

    @Getter
    private final String command;
    private final FunctionThrowing<Long, TelegramContext> fn;

    TelegramCommand(String command, FunctionThrowing<Long, TelegramContext> fn) {
        this.command = command;
        this.fn = fn;
    }

    public TelegramContext getTelegramContext(long idChat) throws Throwable {
        return fn.apply(idChat);
    }

    public static TelegramCommand valueOfCommand(String command) {
        TelegramCommand[] values = values();
        for (TelegramCommand telegramCommand : values) {
            if (telegramCommand.getCommand().equals(command)) {
                return telegramCommand;
            }
        }
        return null;
    }

}
