package ru.jamsys.core.flat.util;

import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.AnswerPreCheckoutQuery;
import org.telegram.telegrambots.meta.api.methods.invoices.SendInvoice;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.payments.LabeledPrice;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo;
import ru.jamsys.core.flat.util.telegram.Button;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class UtilTelegramMessage {

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

    public static String getUserInfo(Update msg) {
        if (msg.hasCallbackQuery()) {
            return UtilJson.toStringPretty(msg.getCallbackQuery().getFrom(), "{}");
        } else if (msg.hasMessage()) {
            return UtilJson.toStringPretty(msg.getMessage().getFrom(), "{}");
        }
        return null;
    }

    public static boolean isBot(Update msg) {
        if (msg.hasCallbackQuery()) {
            return msg.getCallbackQuery().getFrom().getIsBot();
        } else if (msg.hasMessage()) {
            return msg.getMessage().getFrom().getIsBot();
        } else if (msg.hasPreCheckoutQuery()) {
            return false;
        } else {
            // Если какие-то обходные пути будут, лучше прикрыть, так как я не знаю
            return true;
        }
    }

    public static EditMessageText editMessage(Update msg, String data) {
        EditMessageText editMessage = new EditMessageText();
        editMessage.setChatId(getIdChat(msg));
        editMessage.setMessageId(getIdMessage(msg));
        editMessage.setText(data);
        return editMessage;
    }

    @SuppressWarnings("unused")
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

    public static void addMessageButtonList(SendMessage message, List<Button> listButtons) {
        List<List<InlineKeyboardButton>> list = new ArrayList<>();
        listButtons.forEach(button -> {
            InlineKeyboardButton markupInline = new InlineKeyboardButton(button.getData());
            if (button.getWebapp() != null) {
                markupInline.setWebApp(new WebAppInfo(button.getWebapp()));
            }
            if (button.getCallback() != null) {
                markupInline.setCallbackData(button.getCallback());
            }
            if (button.getUrl() != null) {
                markupInline.setUrl(button.getUrl());
            }
            list.add(List.of(markupInline));
        });
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(list);
        message.setReplyMarkup(markup);
    }

    public static SendInvoice getInvoice(
            Long chatId,
            String title,
            String description,
            String rqUid,
            String providerToken,
            String labelPrice,
            int amount,
            String playload
    ) {
        // Создаем инвойс
        SendInvoice sendInvoice = new SendInvoice();
        sendInvoice.setChatId(chatId.toString());
        sendInvoice.setTitle(title);
        sendInvoice.setDescription(description);
        sendInvoice.setPayload(rqUid); // Уникальный идентификатор платежа
        sendInvoice.setProviderToken(providerToken); // Токен платежного провайдера
        sendInvoice.setCurrency("RUB"); // Валюта
        sendInvoice.setPrices(List.of(new LabeledPrice(labelPrice, amount))); // Цена в копейках (10000 = 100 рублей)
        sendInvoice.setStartParameter(playload);

        // Добавляем кнопку "Оплатить"
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton payButton = new InlineKeyboardButton();
        payButton.setText("Оплатить");
        payButton.setPay(true);
        row.add(payButton);
        keyboard.add(row);
        keyboardMarkup.setKeyboard(keyboard);
        sendInvoice.setReplyMarkup(keyboardMarkup);
        return sendInvoice;
    }

    public static AnswerPreCheckoutQuery getPreCheckAnswer(String id, boolean valid) {
        AnswerPreCheckoutQuery answer = new AnswerPreCheckoutQuery();
        answer.setPreCheckoutQueryId(id);
        answer.setOk(valid);
        return answer;
    }

    public static SendMessage message(Long idChat, String data, List<Button> listButtons) {
        SendMessage message = new SendMessage();
        message.setChatId(idChat);
        message.setText(data);
        message.setParseMode("HTML");
        if (listButtons != null && !listButtons.isEmpty()) {
            addMessageButtonList(message, listButtons);
        }
        return message;
    }

}
