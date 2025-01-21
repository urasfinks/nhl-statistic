package ru.jamsys.telegram;

import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.jamsys.core.flat.util.UtilTelegram;
import ru.jamsys.core.flat.util.telegram.Button;

import java.io.InputStream;
import java.util.List;

public interface TelegramSender {

    UtilTelegram.Result send(long idChat, String data, List<Button> buttons);

    UtilTelegram.Result sendImage(long idChat, InputStream is, String fileName, String description);

    void setMyCommands(SetMyCommands setMyCommands) throws TelegramApiException;

}
