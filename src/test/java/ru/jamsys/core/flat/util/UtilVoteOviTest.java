package ru.jamsys.core.flat.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UtilVoteOviTest {

    @Test
    void get() {
        Assertions.assertEquals("""
                🔥 Поздравляем! Вы предсказали гол Ови, и он действительно забил! 🚀
                Следующий матч уже скоро! Попробуйте угадать снова 😉""", UtilVoteOvi.get(10, true));

        Assertions.assertEquals("""
                🚨 Ови забил, но Вы не поверили в него!
                Ничего страшного, бывает! Главное, что ещё один шаг к рекорду сделан.
                Попробуйте угадать в следующий раз 😉""", UtilVoteOvi.get(3, false));

        Assertions.assertEquals("""
                Эх, Ови сегодня не забил, а Вы были уверены в голе…
                Но ничего, главное — поддержка, а следующий матч уже не за горами!
                Давайте попробуем угадать на следующей игре 😉""", UtilVoteOvi.get(0, true));

        Assertions.assertEquals("""
                🎯 Вы были правы — сегодня без голов от Ови.
                Не его день, но ничего страшного! Он ещё покажет, на что способен.
                Скоро новый матч —  попробуйте угадать снова 😉""", UtilVoteOvi.get(0, false));

    }

}