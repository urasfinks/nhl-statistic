package ru.jamsys.core.handler.telegram.eye.card;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.extension.builder.ArrayListBuilder;
import ru.jamsys.core.flat.util.Util;

import java.util.ArrayList;
import java.util.List;

public class SpreadRepository {
    @Getter
    @Setter
    public static class Spread {

        final String title;
        final int min;
        final int max;

        public Spread(String title, int min, int max) {
            this.title = title;
            this.min = min;
            this.max = max;
        }

        public String getSelectCard() {
            if (min == max) {
                return "Выбрать " + min + " " + Util.digitTranslate(min, "карта", "карты", "карт");
            }
            return "Выбрать от " + max + " до " + Util.digitTranslate(max, "карты", "карт", "карт");
        }

    }

    private static final List<Spread> spread = new ArrayListBuilder<Spread>()
            .append(new Spread("🎴 Одна карта (быстрый ответ на конкретный вопрос)", 1, 1))
            .append(new Spread("🎭 Три карты (прошлое – настоящее – будущее)", 3, 3))
            .append(new Spread("🌀 Кельтский крест (глубокий анализ ситуации)", 10, 10))
            .append(new Spread("💞 Расклад на отношения (чувства партнера, перспективы, развитие)", 5, 7))
            .append(new Spread("⚖️ Расклад на выбор (какой путь выбрать)", 4, 4))
            .append(new Spread("🔥 Кармический путь (уроки, кармические задачи)", 7, 10))
            .append(new Spread("🌟 Совет Таро (что делать в сложной ситуации)", 1, 3));

    public static Spread getSpread(int idx) {
        return spread.get(idx);
    }

    public static List<String> getListSpreadTitle() {
        List<String> result = new ArrayList<>();
        spread.forEach((spread) -> {
            result.add(spread.getTitle());
        });
        return result;
    }

}
