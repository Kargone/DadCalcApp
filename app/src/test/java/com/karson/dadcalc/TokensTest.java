package com.karson.dadcalc;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TokensTest {
    @Test
    public void rendersMixedNumbersAndUnits() {
        assertEquals("5 ft 3 1/16 in|", Tokens.render(Arrays.asList(
                "5", "ft", "3", ".", "1", Tokens.FRACTION_BAR, "1", "6", "in", Tokens.CURSOR)));
    }

    @Test
    public void rendersOperatorsAndNegativeSign() {
        assertEquals("\u22122 + 3|", Tokens.render(Arrays.asList(Tokens.SUBTRACT, "2", Tokens.ADD, "3", Tokens.CURSOR)));
    }

    @Test
    public void fromValueRoundTripsThroughEvaluator() {
        assertEquals(Arrays.asList(Tokens.SUBTRACT, "1", ".", "2", "5", "mm"), Tokens.fromValue(-1.25, "mm"));
        assertEquals(Arrays.asList("0", "in"), Tokens.fromValue(0.00000001, "in"));
        assertEquals(-1.25, Evaluator.evaluate(Tokens.fromValue(-1.25, "mm"), "mm"), 1e-9);
    }

    @Test
    public void tapPositionMovesCursor() {
        // Renders as "12 + 3 in|"
        List<String> tokens = new ArrayList<>(Arrays.asList("1", "2", Tokens.ADD, "3", "in", Tokens.CURSOR));
        assertEquals("12 + 3 in|", Tokens.render(tokens));

        Tokens.moveCursorToTextOffset(tokens, 0);
        assertEquals("|12 + 3 in", Tokens.render(tokens));

        // Offsets count the displayed text, cursor included: "|12" puts the gap after "2" at 3
        Tokens.moveCursorToTextOffset(tokens, 3);
        assertEquals("12| + 3 in", Tokens.render(tokens));

        // Tapping just right of the "+" lands after it
        Tokens.moveCursorToTextOffset(tokens, 5);
        assertEquals("12 + |3 in", Tokens.render(tokens));

        Tokens.moveCursorToTextOffset(tokens, 100);
        assertEquals("12 + 3 in|", Tokens.render(tokens));
    }
}
