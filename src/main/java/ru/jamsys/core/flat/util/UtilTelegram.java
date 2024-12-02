package ru.jamsys.core.flat.util;

import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.jamsys.core.flat.util.telegram.Button;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class UtilTelegram {

    @SuppressWarnings("unused")
    public static Integer getIdMessage(Update msg) {
        if (msg.hasCallbackQuery()) {
            return msg.getCallbackQuery().getMessage().getMessageId();
        } else if (msg.hasMessage()) {
            return msg.getMessage().getMessageId();
        }
        return null;
    }

    @SuppressWarnings("unused")
    public static Long getIdChat(Update msg) {
        if (msg.hasCallbackQuery()) {
            return msg.getCallbackQuery().getMessage().getChatId();
        } else if (msg.hasMessage()) {
            return msg.getMessage().getChatId();
        }
        return null;
    }

    @SuppressWarnings("unused")
    public static String getData(Update msg) {
        if (msg.hasCallbackQuery()) {
            return msg.getCallbackQuery().getData();
        } else if (msg.hasMessage()) {
            return msg.getMessage().getText();
        }
        return null;
    }

    public static EditMessageText editMessage(Update msg, String data) {
        EditMessageText editMessage = new EditMessageText();
        editMessage.setChatId(getIdChat(msg));
        editMessage.setMessageId(getIdMessage(msg));
        editMessage.setText(data);
        return editMessage;
    }

    public static DeleteMessage removeMessage(Update msg) {
        DeleteMessage editMessage = new DeleteMessage();
        editMessage.setChatId(Objects.requireNonNull(getIdChat(msg)));
        editMessage.setMessageId(Objects.requireNonNull(getIdMessage(msg)));
        return editMessage;
    }

    public static AnswerCallbackQuery answerCallbackQuery(Update msg, String data) {
        AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery();
        answerCallbackQuery.setCallbackQueryId(msg.getCallbackQuery().getId());
        answerCallbackQuery.setText(data);
        return answerCallbackQuery;
    }

    public static void addMessageButton(SendMessage message, List<Button> listButtons) {
        List<List<InlineKeyboardButton>> list = new ArrayList<>();
        listButtons.forEach(button -> {
            InlineKeyboardButton markupInline = new InlineKeyboardButton(button.getData());
            markupInline.setCallbackData(button.getCallback());
            list.add(List.of(markupInline));
        });
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(list);
        message.setReplyMarkup(markup);
    }

    public static SendMessage message(Long idChat, String data, List<Button> listButtons) {
        SendMessage message = new SendMessage();
        message.setChatId(idChat);
        message.setText(data);
        if (listButtons != null && !listButtons.isEmpty()) {
            addMessageButton(message, listButtons);
        }
        return message;
    }

}
