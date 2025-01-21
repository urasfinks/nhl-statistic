package ru.jamsys.core.flat.util;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.jamsys.core.App;
import ru.jamsys.core.extension.functional.ConsumerThrowing;

public class UtilTelegramResponse {

    public enum ResultException {
        RETRY, // Стоит повторить
        NOT_INIT, // Пользователь не инициализировал бота командой /start
        REVOKE, // Отозвать подписку
        OTHER,
        ID_CHAT_EMPTY,
        SENDER_NULL
    }

    @Getter
    @Setter
    @Accessors(chain = true)
    public static class Result {

        long timeAdd = System.currentTimeMillis();

        @SuppressWarnings("unused")
        public long getTiming() {
            return System.currentTimeMillis() - timeAdd;
        }

        ResultException exception;

        String cause;

        Object response;

        @SuppressWarnings("unused")
        public boolean isOk() {
            return exception == null;
        }

        public boolean isRetry() {
            if (exception == null) { // Если нет исключения - то незамем повторять
                return false;
            }
            return exception.equals(ResultException.RETRY);
        }

    }

    public static Result sandbox(ConsumerThrowing<Result> procedureThrowing) {
        Result telegramResult = new Result();
        try {
            procedureThrowing.accept(telegramResult);
        } catch (Throwable th) {
            if (th.getMessage() == null || th.getMessage().isEmpty()) {
                telegramResult
                        .setException(ResultException.RETRY)
                        .setCause("th.getMessage() is null");
            } else if (th.getMessage().contains("bot can't initiate conversation with a user")) {
                telegramResult
                        .setException(ResultException.NOT_INIT)
                        .setCause(th.getMessage());
            } else if (
                    th.getMessage().contains("Too Many Requests")
                            || th.getMessage().contains("Unable to execute sendmessage method")
                            || th.getMessage().contains("Check your bot token")
            ) {
                telegramResult
                        .setException(ResultException.RETRY)
                        .setCause(th.getMessage());
            } else if (
                    th.getMessage().contains("bot was blocked by the user")
                            || th.getMessage().contains("user is deactivated")
                            || th.getMessage().contains("bot was kicked from the group chat")
                            || th.getMessage().contains("bot can't send messages to bots")
                            || th.getMessage().contains("bot is not a member of the channel chat")
            ) {
                telegramResult
                        .setException(ResultException.REVOKE)
                        .setCause(th.getMessage());
            } else {
                telegramResult
                        .setException(ResultException.OTHER)
                        .setCause(th.getMessage());
                App.error(th);
            }
        }
        return telegramResult;
    }

}
