package com.karson.dadcalc;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class OutputFormatterTest {
    @Test
    public void decimals() {
        assertEquals("1.5 in", OutputFormatter.format(1.5, "in", false, false, 16));
        assertEquals("0.33333 mm", OutputFormatter.format(1.0 / 3, "mm", false, false, 16));
        assertEquals("-0.5 in", OutputFormatter.format(-0.5, "in", false, false, 16));
        assertEquals("0 in", OutputFormatter.format(-0.00000001, "in", false, false, 16));
    }

    @Test
    public void fractions() {
        assertEquals("1 1/2 in", OutputFormatter.format(1.5, "in", true, false, 16));
        assertEquals("3/16 in", OutputFormatter.format(0.1875, "in", true, false, 16));
        assertEquals("2 in", OutputFormatter.format(2, "in", true, false, 16));
    }

    @Test
    public void rounding() {
        assertEquals("0.125 in", OutputFormatter.format(0.1, "in", false, true, 16));
        assertEquals("1/8 in", OutputFormatter.format(0.1, "in", true, true, 16));
        assertEquals("1/4 in", OutputFormatter.format(0.2, "in", true, true, 4));
    }

    @Test
    public void feetAndInches() {
        assertEquals("5 ft 3 in", OutputFormatter.format(5.25, "ft", false, false, 16));
        assertEquals("6 ft 0 in", OutputFormatter.format(5.9999999, "ft", true, false, 16));
        assertEquals("1 ft 1/2 in", OutputFormatter.format(1 + 0.5 / 12, "ft", true, false, 16));
        assertEquals("-1 ft 6 in", OutputFormatter.format(-1.5, "ft", false, false, 16));
    }
}
