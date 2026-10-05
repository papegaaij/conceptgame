package vanguard.game.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CreditNumbersTest {
    private final CreditNumbers numbers = new CreditNumbers();

    @Test
    void showsEveryNumberByDefault() {
        numbers.show(25, 100, 200);
        numbers.show(10, 140, 220);

        assertEquals(2, numbers.drawn());
    }

    @Test
    void theOptionHidesTheNumbers() {
        numbers.show(25, 100, 200);

        numbers.visible(false);

        assertEquals(0, numbers.drawn());
    }

    @Test
    void turnedBackOnItShowsOnlyTheNumbersStillRising() {
        numbers.visible(false);
        numbers.show(25, 100, 200);
        for (int i = 0; i < 59; i++) {
            numbers.step();
        }
        numbers.show(10, 140, 220);
        numbers.step();

        numbers.visible(true);

        assertEquals(1, numbers.drawn());
    }
}
