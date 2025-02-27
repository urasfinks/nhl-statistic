package ru.jamsys.core.handler.telegram.eye.card;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class RiderWaiteTarotDeckCardTest {
    @Test
    public void test() {
        RiderWaiteTarotDeckCard riderWaiteTarotDeckCard = new RiderWaiteTarotDeckCard();
        Assertions.assertEquals(78, riderWaiteTarotDeckCard.getSize());
    }
}