package ru.jamsys.telegram;

import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.jamsys.core.flat.util.UtilTelegramResponse;
import ru.jamsys.core.flat.util.telegram.Button;

import java.io.InputStream;
import java.util.List;

public interface TelegramSender {

    UtilTelegramResponse.Result send(long idChat, String data, List<Button> buttons);

    UtilTelegramResponse.Result sendImage(long idChat, InputStream is, String fileName, String description);

    UtilTelegramResponse.Result sendImage(long idChat, String idFile, String description);

    UtilTelegramResponse.Result sendVideo(long idChat, String idFile, String description);

    void setMyCommands(SetMyCommands setMyCommands) throws TelegramApiException;

    TelegramSender setNotCommandPrefix(String notCommandPrefix);

}
