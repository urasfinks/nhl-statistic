package ru.jamsys.core.handler.telegram.eye.card;

import lombok.Getter;
import ru.jamsys.core.extension.builder.ArrayListBuilder;


@Getter
public abstract class AbstractDeckCard {

    private final ArrayListBuilder<Card> cards = new ArrayListBuilder<>();

    final String name;

    protected AbstractDeckCard(String name) {
        this.name = name;
    }

    public int getSize() {
        return cards.size();
    }

}
