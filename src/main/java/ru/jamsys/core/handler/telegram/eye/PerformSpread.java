package ru.jamsys.core.handler.telegram.eye;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.NhlStatisticApplication;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.manager.item.Session;
import ru.jamsys.core.extension.builder.ArrayListBuilder;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.http.ServletResponseWriter;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.telegram.Button;
import ru.jamsys.core.handler.promise.RegisterNotification;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.telegram.TelegramCommandContext;
import ru.jamsys.telegram.TelegramNotification;
import ru.jamsys.telegram.handler.EyeBotCommandHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
@Setter
@Getter
@Component
@Lazy
@RequestMapping("/perform_spread/**")
public class PerformSpread implements PromiseGenerator, EyeBotCommandHandler {

    private final ServicePromise servicePromise;

    private final Session<Long, Context> mapSession;

    public PerformSpread(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
        mapSession = new Session<>(getClass().getSimpleName(), 6_000_000L);
    }

    @Getter
    @Setter
    public static class Context {

        private final Map<String, String> param;

        private final List<Integer> card = new ArrayList<>();

        public Context(Map<String, String> param) {
            this.param = param;
        }

    }

    @Override
    public Promise generate() {
        return servicePromise.get(getClass().getSimpleName(), 12_000L)
                .then("start", (_, _, promise) -> {
                    TelegramCommandContext telegramContext = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    Context context = promise.getRepositoryMapClass(Context.class);
                    for (String step : titleQuestion.keySet()) {
                        if (!telegramContext.getUriParameters().containsKey(step)) {
                            RegisterNotification.add(new TelegramNotification(
                                    telegramContext.getIdChat(),
                                    telegramContext.getTelegramBot().getBotUsername(),
                                    getSelected(telegramContext.getUriParameters())
                                            + titleQuestion.get(step).getQuestion(),
                                    getStepButtons(step, telegramContext.getUriParameters()),
                                    null
                            ));
                            promise.skipAllStep("wait " + step);
                            return;
                        }
                    }
                    Context session = mapSession.computeIfAbsent(telegramContext.getIdChat(), aLong -> new Context(telegramContext.getUriParameters()));
                    if (telegramContext.getUriParameters().containsKey("question")) {
                        session.getParam().put("question", telegramContext.getUriParameters().get("question"));
                        //Так как question большой, удаляем его из параметров запроса
                        telegramContext.getUriParameters().remove("question");
                    }
                    if (!session.getParam().containsKey("question")) {
                        telegramContext.getStepHandler().put(
                                telegramContext.getIdChat(),
                                ServletResponseWriter.buildUrlQuery(
                                        "/perform_spread/",
                                        telegramContext.getUriParameters()
                                ) + "&question="
                        );
                        RegisterNotification.add(new TelegramNotification(
                                telegramContext.getIdChat(),
                                telegramContext.getTelegramBot().getBotUsername(),
                                "Задайте вопрос колоде:",
                                null,
                                null
                        ));
                        promise.skipAllStep("wait question");
                        return;
                    }
                    if (telegramContext.getUriParameters().containsKey("card")) {
                        session.getParam().put("card", telegramContext.getUriParameters().get("card"));
                        //Так как question большой, удаляем его из параметров запроса
                        telegramContext.getUriParameters().remove("card");
                    }
                    if (!session.getParam().containsKey("card")) {
                        telegramContext.getStepHandler().put(
                                telegramContext.getIdChat(),
                                ServletResponseWriter.buildUrlQuery(
                                        "/perform_spread/",
                                        telegramContext.getUriParameters()
                                ) + "&card="
                        );
                        RegisterNotification.add(new TelegramNotification(
                                telegramContext.getIdChat(),
                                telegramContext.getTelegramBot().getBotUsername(),
                                getSelected(telegramContext.getUriParameters()) +
                                        "Колода услышала ваш вопрос: " + session.getParam().get("question") + ".\nКолода перемешана, в колоде 78 карт, напишите 3 номера карт, через запятую (,), которые вытянуть:",
                                null,
                                null
                        ));
                        promise.skipAllStep("wait card");
                        return;
                    }
                    String[] cards = session.getParam().get("card").split(",");
                    session.getCard().clear();
                    for (String cardNum : cards) {
                        if (Util.isNumeric(cardNum.trim())) {
                            session.getCard().add(Integer.parseInt(cardNum.trim()));
                        }
                    }
                    if (session.getCard().size() < 3) {
                        session.getParam().remove("card");
                        telegramContext.getStepHandler().put(
                                telegramContext.getIdChat(),
                                ServletResponseWriter.buildUrlQuery(
                                        "/perform_spread/",
                                        telegramContext.getUriParameters()
                                ) + "&card="
                        );
                        RegisterNotification.add(new TelegramNotification(
                                telegramContext.getIdChat(),
                                telegramContext.getTelegramBot().getBotUsername(),
                                getSelected(telegramContext.getUriParameters()) +
                                        "Колода услышала ваш вопрос: " + session.getParam().get("question") + ".\nКолода перемешана, в колоде 78 карт, напишите 3 номера карт, через запятую (,), которые вытянуть:",
                                null,
                                null
                        ));
                        promise.skipAllStep("wait card");
                        return;
                    }
                    Util.logConsoleJson(getClass(), session);
                })
                .extension(NhlStatisticApplication::addOnError);
    }

    @Getter
    @Setter
    @Accessors(chain = true)
    public static class Steps {
        String question;
        String title;
        ArrayListBuilder<String> variant = new ArrayListBuilder<>();
    }

    public static Map<String, Steps> titleQuestion = new HashMapBuilder<String, Steps>()
            .append("s1", new Steps()
                    .setQuestion("Выберите область, на которую хотите получить предсказание:")
                    .setTitle("Область")
                    .setVariant(new ArrayListBuilder<String>()
                            .append("🏹 Личная жизнь (любовь, отношения, поиск партнера)")
                            .append("💼 Работа и карьера (работа, повышение, выбор профессии)")
                            .append("💰 Финансы (денежные вопросы, вложения, материальное благополучие)")
                            .append("⚕️ Здоровье (общее самочувствие, энергетический баланс)")
                            .append("🔮 Духовный путь (саморазвитие, предназначение, внутренние трансформации)")
                            .append("⚖️ Ситуация или выбор (важное решение, оценка перспектив)"))
            )
            .append("s2", new Steps()
                    .setQuestion("Выберите тип расклада:")
                    .setTitle("Тип расклада")
                    .setVariant(new ArrayListBuilder<String>()
                            .append("🎴 Одна карта (быстрый ответ на конкретный вопрос)")
                            .append("🎭 Три карты (прошлое – настоящее – будущее)")
                            .append("🌀 Кельтский крест (глубокий анализ ситуации)")
                            .append("💞 Расклад на отношения (чувства партнера, перспективы, развитие)")
                            .append("⚖️ Расклад на выбор (какой путь выбрать)")
                            .append("🔥 Кармический путь (уроки, кармические задачи)")
                            .append("🌟 Совет Таро (что делать в сложной ситуации)")
                    )
            )
            .append("s3", new Steps()
                    .setQuestion("Определите временные рамки:")
                    .setTitle("Временные рамки")
                    .setVariant(new ArrayListBuilder<String>()
                            .append("📅 На день")
                            .append("📆 На неделю")
                            .append("🗓 На месяц")
                            .append("📖 На год")
                            .append("🔮 На неопределенное будущее")
                            .append("⏳ Долгосрочная перспектива")
                    )
            )
            .append("s4", new Steps()
                    .setQuestion("Выберите колоду:")
                    .setTitle("Колода")
                    .setVariant(new ArrayListBuilder<String>()
                            .append("🏰 Таро Райдер-Уэйта (классика)")
                            .append("🎭 Таро Тота (Алистера Кроули)")
                            .append("💋 Таро Манара (любовные и интимные вопросы)")
                            .append("🌲 Таро Друидов (природные энергии, магия)")
                            .append("🔥 Таро Теней (глубинный анализ, карма)")
                            .append("🃏 Оракул Ленорман (не совсем Таро, но тоже предсказательная система)")
                    )
            )
            .append("s5", new Steps()
                    .setQuestion("Дополнительные пожелания")
                    .setTitle("Дополнительные пожелания")
                    .setVariant(new ArrayListBuilder<String>()
                            .append("📖 Полное разъяснение (анализ ситуации + прогноз)")
                            .append("⚖️ Сравнение вариантов (если выбор между двумя решениями)")
                            .append("🌟 Конкретный совет от Таро")
                            .append("🔥 Проработка кармических узлов и прошлых жизней")
                            .append("💞 Расшифровка эмоционального состояния партнера")
                            .append("🌀 Что мешает и что поможет вам в этой ситуации")
                    )
            );

    public String getSelected(Map<String, String> param) {
        StringBuilder sb = new StringBuilder();
        param.forEach((key, idx) -> {
            if (titleQuestion.get(key) == null) {
                return;
            }
            sb.append(getInfo(
                    titleQuestion.get(key).getVariant(),
                    Integer.parseInt(idx),
                    titleQuestion.get(key).getTitle()
            )).append("\n");
        });
        return sb.append("\n").toString();
    }

    public static String getInfo(List<String> list, int idx, String title) {
        return "🔸 " + title + ": " + list.get(idx);
    }

    public static List<Button> getStepButtons(String key, Map<String, String> param) {
        List<String> list = titleQuestion.get(key).getVariant();
        List<Button> result = new ArrayList<>();
        int idx = 0;
        for (String item : list) {
            result.add(new Button(
                    item,
                    ServletResponseWriter.buildUrlQuery(
                            "/perform_spread/",
                            new HashMapBuilder<>(param)
                                    .append(key, "" + idx++)
                    )
            ));
        }
        return result;
    }

}
