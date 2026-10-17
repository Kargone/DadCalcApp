package com.karson.dadcalc;

import java.util.ArrayList;
import java.util.List;

/**
 * Evaluates the input tokens (see {@link Tokens}) to a single length in the output unit.
 *
 * Grammar, loosest binding first:
 *   expression  := term (("+" | "−") term)*
 *   term        := factor (("×" | "÷") factor | factor right after a ")" or before a "(")*
 *   factor      := ("−" | "+") factor | measurement
 *   measurement := quantity quantity*       quantities after a unit add up, so "5 ft 3 in" works
 *   quantity    := "(" expression [")"] [unit] | number [unit]
 *   number      := digits ["." digits] ["⁄" digits]
 *
 * Numbers without a unit are inches, the same as the web version.
 */
public final class Evaluator {
    private final List<String> tokens;
    private final String outputUnit;
    private int position = 0;
    private boolean lastQuantityHadUnit = false;

    private Evaluator(List<String> tokens, String outputUnit) {
        this.tokens = tokens;
        this.outputUnit = outputUnit;
    }

    /** @throws IllegalArgumentException if the input isn't a valid calculation */
    public static double evaluate(List<String> input, String outputUnit) {
        List<String> tokens = new ArrayList<>(input);
        tokens.remove(Tokens.CURSOR);
        // Ignore a half-typed ending like "5 +" so the output still shows something while typing
        while (!tokens.isEmpty() && isUnfinished(tokens.get(tokens.size() - 1))) {
            tokens.remove(tokens.size() - 1);
        }
        if (tokens.isEmpty()) return 0;

        Evaluator evaluator = new Evaluator(tokens, outputUnit);
        double result = evaluator.parseExpression();
        if (evaluator.peek() != null) throw new IllegalArgumentException("Unexpected token: " + evaluator.peek());
        if (Double.isNaN(result) || Double.isInfinite(result)) throw new IllegalArgumentException("Result is not a number");
        return result;
    }

    private static boolean isUnfinished(String token) {
        return Tokens.isOperator(token) || Tokens.FRACTION_BAR.equals(token) || Tokens.OPEN.equals(token);
    }

    private double parseExpression() {
        double value = parseTerm();
        while (true) {
            if (accept(Tokens.ADD)) value += parseTerm();
            else if (accept(Tokens.SUBTRACT)) value -= parseTerm();
            else return value;
        }
    }

    private double parseTerm() {
        double value = parseFactor();
        while (true) {
            if (accept(Tokens.MULTIPLY)) value *= parseFactor();
            else if (accept(Tokens.DIVIDE)) value /= parseFactor();
            else if (Tokens.OPEN.equals(peek()) || (followsGroup() && startsNumber(peek()))) value *= parseFactor();
            else return value;
        }
    }

    private double parseFactor() {
        if (accept(Tokens.SUBTRACT)) return -parseFactor();
        if (accept(Tokens.ADD)) return parseFactor();
        return parseMeasurement();
    }

    private double parseMeasurement() {
        double total = parseQuantity();
        while (lastQuantityHadUnit && startsNumber(peek())) {
            total += parseQuantity();
        }
        return total;
    }

    private double parseQuantity() {
        if (accept(Tokens.OPEN)) {
            double value = parseExpression();
            // A missing ")" is fine at the very end so "(2 + 3" still previews
            if (!accept(Tokens.CLOSE) && peek() != null) throw new IllegalArgumentException("Expected ): " + peek());
            // A unit after a group is ignored, the same as the web version
            if (Convert.isUnit(peek())) position++;
            lastQuantityHadUnit = false;
            return value;
        }
        double value = parseNumber();
        String unit = Convert.INCHES;
        lastQuantityHadUnit = Convert.isUnit(peek());
        if (lastQuantityHadUnit) unit = tokens.get(position++);
        return Convert.x_y(value, unit, outputUnit);
    }

    private double parseNumber() {
        String whole = readDigits();
        boolean hasDecimal = accept(Tokens.DECIMAL);
        String afterDecimal = hasDecimal ? readDigits() : "";
        if (whole.isEmpty() && afterDecimal.isEmpty()) throw new IllegalArgumentException("Expected a number: " + peek());

        if (!accept(Tokens.FRACTION_BAR)) {
            return Double.parseDouble((whole.isEmpty() ? "0" : whole) + "." + (afterDecimal.isEmpty() ? "0" : afterDecimal));
        }
        String denominator = readDigits();
        if (denominator.isEmpty()) throw new IllegalArgumentException("Fraction is missing a denominator");
        double denominatorValue = Double.parseDouble(denominator);
        if (denominatorValue == 0) throw new IllegalArgumentException("Fraction denominator is zero");
        if (!hasDecimal) return Double.parseDouble(whole) / denominatorValue;
        // "5.3⁄16" is the mixed number 5 3/16
        if (afterDecimal.isEmpty()) throw new IllegalArgumentException("Mixed number is missing a numerator");
        return (whole.isEmpty() ? 0 : Double.parseDouble(whole)) + Double.parseDouble(afterDecimal) / denominatorValue;
    }

    private String readDigits() {
        StringBuilder digits = new StringBuilder();
        while (Tokens.isDigit(peek())) digits.append(tokens.get(position++));
        return digits.toString();
    }

    private static boolean startsNumber(String token) {
        return Tokens.isDigit(token) || Tokens.DECIMAL.equals(token);
    }

    private boolean followsGroup() {
        return position > 0 && Tokens.CLOSE.equals(tokens.get(position - 1));
    }

    private String peek() {
        return position < tokens.size() ? tokens.get(position) : null;
    }

    private boolean accept(String token) {
        if (!token.equals(peek())) return false;
        position++;
        return true;
    }
}
