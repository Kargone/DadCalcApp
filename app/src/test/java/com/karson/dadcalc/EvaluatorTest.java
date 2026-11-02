package com.karson.dadcalc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class EvaluatorTest {
    private static final double DELTA = 1e-9;
    private static final String F = Tokens.FRACTION_BAR;
    private static final String C = Tokens.CURSOR;

    private static List<String> tokens(String... tokens) {
        return Arrays.asList(tokens);
    }

    private static double evaluate(String unit, String... tokens) {
        return Evaluator.evaluate(tokens(tokens), unit);
    }

    @Test
    public void emptyInputIsZero() {
        assertEquals(0, evaluate("in"), DELTA);
        assertEquals(0, evaluate("in", C), DELTA);
    }

    @Test
    public void cursorIsIgnoredWherever() {
        assertEquals(15, evaluate("in", "1", C, "2", "+", "3"), DELTA);
    }

    @Test
    public void multiplicationBeforeAddition() {
        assertEquals(14, evaluate("in", "2", "+", "3", Tokens.MULTIPLY, "4"), DELTA);
        assertEquals(1, evaluate("in", "9", Tokens.DIVIDE, "3", Tokens.SUBTRACT, "2"), DELTA);
    }

    @Test
    public void parenthesesWithImplicitMultiply() {
        assertEquals(14, evaluate("in", "2", "(", "3", "+", "4", ")"), DELTA);
        assertEquals(10, evaluate("in", "(", "2", ")", "5"), DELTA);
    }

    @Test
    public void unfinishedInputStillPreviews() {
        assertEquals(5, evaluate("in", "(", "2", "+", "3"), DELTA);
        assertEquals(5, evaluate("in", "5", "+"), DELTA);
        assertEquals(3, evaluate("in", "3", F), DELTA);
    }

    @Test
    public void negativeNumbers() {
        assertEquals(2, evaluate("in", Tokens.SUBTRACT, "3", "+", "5"), DELTA);
        assertEquals(-6, evaluate("in", "2", Tokens.MULTIPLY, Tokens.SUBTRACT, "3"), DELTA);
    }

    @Test
    public void unitsConvertToOutputUnit() {
        assertEquals(18, evaluate("in", "1", "ft", "+", "6", "in"), DELTA);
        assertEquals(1.5, evaluate("ft", "1", "ft", "+", "6", "in"), DELTA);
        assertEquals(1, evaluate("in", "2", "5", ".", "4", "mm"), DELTA);
        assertEquals(25.4, evaluate("mm", "1"), DELTA);
    }

    @Test
    public void adjacentMeasurementsAddUp() {
        assertEquals(63, evaluate("in", "5", "ft", "3", "in"), DELTA);
        assertEquals(63, evaluate("in", "5", "ft", "3"), DELTA);
    }

    @Test
    public void unitAfterParenthesesAppliesToTheWholeGroup() {
        assertEquals(5, evaluate("ft", "(", "2", "+", "3", ")", "ft"), DELTA);
        assertEquals(6, evaluate("ft", "(", "2", Tokens.MULTIPLY, "3", ")", "ft"), DELTA);
        assertEquals(10, evaluate("mm", "(", "2", "+", "3", ")", "mm", Tokens.MULTIPLY, "2"), DELTA);
        // Plain numbers inside take the group's unit, typed units keep theirs
        assertEquals(27, evaluate("in", "(", "2", "+", "3", "in", ")", "ft"), DELTA);
        // "(2 + 3) ft 6" is 5 ft 6 in
        assertEquals(66, evaluate("in", "(", "2", "+", "3", ")", "ft", "6"), DELTA);
    }

    @Test
    public void lengthDividedByLengthIsHowManyFitInTheFirstUnit() {
        assertEquals(5, evaluate("ft", "1", "0", "ft", Tokens.DIVIDE, "2", "ft"), DELTA);
        assertEquals(60, evaluate("in", "1", "0", "ft", Tokens.DIVIDE, "2", "ft"), DELTA);
        assertEquals(20, evaluate("ft", "1", "0", "ft", Tokens.DIVIDE, "6", "in"), DELTA);
        assertEquals(4, evaluate("mm", "1", "0", "0", "mm", Tokens.DIVIDE, "2", "5", "mm"), DELTA);
        // The count works like a plain number afterwards: 5 × 3 ft and 5 ft + 1 ft
        assertEquals(15, evaluate("ft", "(", "1", "0", "ft", Tokens.DIVIDE, "2", "ft", ")", Tokens.MULTIPLY, "3", "ft"), DELTA);
        assertEquals(6, evaluate("ft", "1", "0", "ft", Tokens.DIVIDE, "2", "ft", "+", "1", "ft"), DELTA);
        // Length ÷ plain number and plain ÷ plain are unchanged
        assertEquals(5, evaluate("ft", "1", "0", "ft", Tokens.DIVIDE, "2"), DELTA);
        assertEquals(5, evaluate("in", "1", "0", Tokens.DIVIDE, "2"), DELTA);
    }

    @Test
    public void multiplyingGivesTheSameLengthInEveryOutputUnit() {
        // 6 × 1/2 × 955 is 2865 in, which is 238.75 ft, not 1 ft 7.9 in
        String[] input = {"6", Tokens.MULTIPLY, "1", F, "2", Tokens.MULTIPLY, "9", "5", "5"};
        assertEquals(2865, evaluate("in", input), DELTA);
        assertEquals(238.75, evaluate("ft", input), DELTA);
        assertEquals(2865 * 25.4, evaluate("mm", input), 1e-6);
        assertEquals(10, evaluate("ft", "1", "0", "ft", Tokens.DIVIDE, "2", Tokens.MULTIPLY, "2"), DELTA);
    }

    @Test
    public void fractions() {
        assertEquals(0.1875, evaluate("in", "3", F, "1", "6"), DELTA);
        assertEquals(5.1875, evaluate("in", "5", ".", "3", F, "1", "6"), DELTA);
        assertEquals(25.4 * 3 / 16, evaluate("mm", "3", F, "1", "6", "in"), DELTA);
    }

    @Test
    public void invalidInputThrows() {
        assertThrows(IllegalArgumentException.class, () -> evaluate("in", "1", Tokens.DIVIDE, "0"));
        assertThrows(IllegalArgumentException.class, () -> evaluate("in", "in"));
        assertThrows(IllegalArgumentException.class, () -> evaluate("in", "5", ".", "3", ".", "2"));
        assertThrows(IllegalArgumentException.class, () -> evaluate("in", "5", ")"));
        assertThrows(IllegalArgumentException.class, () -> evaluate("in", "3", F, "0"));
    }
}
