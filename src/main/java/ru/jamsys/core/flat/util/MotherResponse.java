package ru.jamsys.core.flat.util;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Accessors(chain = true)
public class MotherResponse {

    private List<String> recommendations = new ArrayList<>();

    private String clarification; // Уточнить вопрос

    private String error;

    public boolean isClarification() {
        return clarification != null && !clarification.isEmpty();
    }

    private boolean retry = false;

    public boolean isError() {
        return error != null && !error.isEmpty();
    }

    public String getRec() {
        if (recommendations.isEmpty()) {
            return "К сожалению, ничего посоветовать не могу";
        }
        StringBuilder sb = new StringBuilder();
        recommendations.forEach(s -> sb.append("🔸 ").append(s).append("\n"));
        sb.append("\nЕсли есть ещё вопросы, я с радостью подскажу! 😊 /ask_question");
        return sb.toString();
    }


}
