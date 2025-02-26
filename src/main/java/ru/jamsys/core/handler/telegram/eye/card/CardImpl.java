package ru.jamsys.core.handler.telegram.eye.card;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CardImpl implements Card {

    final String group;
    final String title;
    final String description;
    final String image;

    public CardImpl(String group, String title, String description, String image) {
        this.group = group;
        this.title = title;
        this.description = description;
        this.image = image;
    }

}
