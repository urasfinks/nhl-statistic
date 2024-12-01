package ru.jamsys;

import lombok.Getter;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.jamsys.core.App;
import ru.jamsys.core.extension.builder.ArrayListBuilder;
import ru.jamsys.core.flat.util.UtilJson;

import java.util.ArrayList;
import java.util.List;

public class TelegramBotHandler extends TelegramLongPollingBot {

    public TelegramBotHandler(String botToken) throws TelegramApiException {
        super(botToken);
        List<BotCommand> list = new ArrayList<>();
        list.add(new BotCommand("/subscribe_to_player", "Подписаться на игрока"));
        list.add(new BotCommand("/my_subscriptions", "Мои подписки"));
        list.add(new BotCommand("/remove_subscription", "Удалить подписку"));
        this.execute(new SetMyCommands(list, new BotCommandScopeDefault(), null));
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            System.out.println(UtilJson.toStringPretty(update, "{}"));
            if (update != null) {
                if (update.hasCallbackQuery()) {
                    handleCallbackQuery(update.getCallbackQuery());
                } else if (update.hasMessage() && update.getMessage().getFrom().getId() == 290029195) {
                    SendMessage message = new SendMessage();
                    message.setChatId(update.getMessage().getChatId());
                    message.setText("Hi! I'm a Telegram bot.'");
                    addMessageButton(message, new ArrayListBuilder<Button>()
                            .append(new Button("Привет", "р1"))
                            .append(new Button("Страна", "р2"))
                    );
                    execute(message);
                }
            }
        } catch (Throwable e) {
            App.error(e);
        }
    }

    @Getter
    public static class Button {

        private final String data;
        private final String callback;

        public Button(String data, String callback) {
            this.data = data;
            this.callback = callback;
        }

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

    private void handleCallbackQuery(CallbackQuery callbackQuery) throws TelegramApiException {
        String callbackData = callbackQuery.getData(); // Получаем данные из callback_data
        String chatId = callbackQuery.getMessage().getChatId().toString();
        Integer messageId = callbackQuery.getMessage().getMessageId();

        // Обязательно отправляем ответ на callbackQuery, чтобы остановить "загрузку"
        AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery();
        answerCallbackQuery.setCallbackQueryId(callbackQuery.getId());
        answerCallbackQuery.setText("...");
        execute(answerCallbackQuery);

        // Дополнительно — редактируем сообщение, чтобы показать результат
        EditMessageText editMessage = new EditMessageText();
        editMessage.setChatId(chatId);
        editMessage.setMessageId(messageId);
        editMessage.setText(callbackData); // Показываем, что выбрано

        execute(editMessage);
    }

    @Override
    public String getBotUsername() {
        return "ovi_goals_bot";
    }

}
