package com.karson.dadcalc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class ConvertTest {
    private static final double DELTA = 1e-9;

    @Test
    public void convertsBetweenUnits() {
        assertEquals(25.4, Convert.x_y(1, "in", "mm"), DELTA);
        assertEquals(12, Convert.x_y(1, "ft", "in"), DELTA);
        assertEquals(304.8, Convert.x_y(1, "ft", "mm"), DELTA);
        assertEquals(1, Convert.x_y(304.8, "mm", "ft"), DELTA);
        assertEquals(7, Convert.x_y(7, "mm", "mm"), DELTA);
    }

    @Test
    public void rejectsUnknownUnits() {
        assertThrows(IllegalArgumentException.class, () -> Convert.x_y(1, "in", "cm"));
    }
}
