package ru.jamsys.core.flat.util.telegram;

import lombok.Getter;

@Getter
public class Button {

    private final String data;

    private final String callback;

    public Button(String data, String callback) {
        this.data = data;
        this.callback = callback;
    }

}