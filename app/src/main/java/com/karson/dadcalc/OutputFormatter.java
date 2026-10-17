package com.karson.dadcalc;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class OutputFormatter {
    private static final int MAX_FRACTION_DENOMINATOR = 64;
    private static final double FRACTION_TOLERANCE = 1e-5;
    /** Anything this close to 12 inches would be displayed as "12", so it carries into the feet. */
    private static final double INCH_CARRY_THRESHOLD = 12 - 5e-6;

    private OutputFormatter() {}

    public static String format(double value, String unit, boolean doFractionOutput, boolean roundOutput, int roundTo) {
        String sign = value < 0 ? "-" : "";
        double magnitude = Math.abs(value);

        if (Convert.FEET.equals(unit)) {
            long feet = (long) magnitude;
            double inches = tidy((magnitude - feet) * 12, roundOutput, roundTo);
            if (inches >= INCH_CARRY_THRESHOLD) {
                feet++;
                inches = 0;
            }
            if (feet == 0 && inches == 0) sign = "";
            return sign + feet + " ft " + formatNumber(inches, doFractionOutput) + " in";
        }

        double tidied = tidy(magnitude, roundOutput, roundTo);
        if (tidied == 0) sign = "";
        return sign + formatNumber(tidied, doFractionOutput) + " " + unit;
    }

    /** Rounds to the nearest 1/roundTo when rounding is on, otherwise just removes floating point noise. */
    private static double tidy(double value, boolean roundOutput, int roundTo) {
        if (roundOutput) return (double) Math.round(value * roundTo) / roundTo;
        return (double) Math.round(value * 10000000) / 10000000;
    }

    private static String formatNumber(double value, boolean doFractionOutput) {
        if (doFractionOutput) {
            long whole = (long) value;
            double remainder = value - whole;
            for (int denominator = 1; denominator <= MAX_FRACTION_DENOMINATOR; denominator++) {
                long numerator = Math.round(remainder * denominator);
                if (Math.abs(remainder * denominator - numerator) > FRACTION_TOLERANCE) continue;
                if (numerator == 0) return String.valueOf(whole);
                if (numerator == denominator) return String.valueOf(whole + 1);
                return (whole == 0 ? "" : whole + " ") + numerator + "/" + denominator;
            }
        }
        return new DecimalFormat("0.#####", DecimalFormatSymbols.getInstance(Locale.US)).format(value);
    }
}
