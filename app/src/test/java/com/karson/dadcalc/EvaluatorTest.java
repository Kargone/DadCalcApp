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
