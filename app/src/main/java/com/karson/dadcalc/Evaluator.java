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
 * Lengths are worked out in inches and only the final answer is converted, so the answer is the same
 * length whatever the output unit. A number without a unit stays a plain number until it meets a length
 * or is the answer, and then it's in the default unit: inches, or the unit after its group, so
 * "(2 × 3) ft" is 6 ft. Dividing a length by a length gives how many fit, in the first one's unit,
 * so "10 ft ÷ 2 ft" is 5 ft.
 */
public final class Evaluator {
    private final List<String> tokens;
    private int position = 0;
    private boolean lastQuantityHadUnit = false;
    private String defaultUnit = Convert.INCHES;

    /** A plain number, or a length in inches. The unit is the one it was typed in, or null when mixed or none. */
    private static final class Value {
        final double number;
        final boolean isLength;
        final String unit;

        Value(double number, boolean isLength, String unit) {
            this.number = number;
            this.isLength = isLength;
            this.unit = unit;
        }
    }

    private Evaluator(List<String> tokens) {
        this.tokens = tokens;
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

        Evaluator evaluator = new Evaluator(tokens);
        double inches = evaluator.toLength(evaluator.parseExpression()).number;
        if (evaluator.peek() != null) throw new IllegalArgumentException("Unexpected token: " + evaluator.peek());
        if (Double.isNaN(inches) || Double.isInfinite(inches)) throw new IllegalArgumentException("Result is not a number");
        return Convert.x_y(inches, Convert.INCHES, outputUnit);
    }

    private static boolean isUnfinished(String token) {
        return Tokens.isOperator(token) || Tokens.FRACTION_BAR.equals(token) || Tokens.OPEN.equals(token);
    }

    private Value parseExpression() {
        Value value = parseTerm();
        while (true) {
            if (accept(Tokens.ADD)) value = add(value, parseTerm(), 1);
            else if (accept(Tokens.SUBTRACT)) value = add(value, parseTerm(), -1);
            else return value;
        }
    }

    private Value parseTerm() {
        Value value = parseFactor();
        while (true) {
            if (accept(Tokens.MULTIPLY)) value = multiply(value, parseFactor());
            else if (accept(Tokens.DIVIDE)) value = divide(value, parseFactor());
            else if (Tokens.OPEN.equals(peek()) || (followsGroup() && startsNumber(peek()))) value = multiply(value, parseFactor());
            else return value;
        }
    }

    private Value parseFactor() {
        if (accept(Tokens.SUBTRACT)) {
            Value value = parseFactor();
            return new Value(-value.number, value.isLength, value.unit);
        }
        if (accept(Tokens.ADD)) return parseFactor();
        return parseMeasurement();
    }

    private Value parseMeasurement() {
        Value total = parseQuantity();
        while (lastQuantityHadUnit && startsNumber(peek())) {
            // A bare number here is the inches in "5 ft 3"
            Value next = parseQuantity();
            if (!next.isLength) next = inUnit(next.number, Convert.INCHES);
            total = add(total, next, 1);
        }
        return total;
    }

    private Value parseQuantity() {
        if (accept(Tokens.OPEN)) {
            // A unit after the group is the unit for the plain numbers inside it, so "(2 + 3) ft" is 5 ft
            String groupUnit = unitAfterGroup();
            String outerDefaultUnit = defaultUnit;
            if (groupUnit != null) defaultUnit = groupUnit;
            Value value = parseExpression();
            defaultUnit = outerDefaultUnit;
            // A missing ")" is fine at the very end so "(2 + 3" still previews
            if (!accept(Tokens.CLOSE) && peek() != null) throw new IllegalArgumentException("Expected ): " + peek());
            lastQuantityHadUnit = groupUnit != null;
            if (groupUnit == null) return value;
            position++;
            return value.isLength ? value : inUnit(value.number, groupUnit);
        }
        double number = parseNumber();
        lastQuantityHadUnit = Convert.isUnit(peek());
        if (!lastQuantityHadUnit) return new Value(number, false, null);
        return inUnit(number, tokens.get(position++));
    }

    /** The unit right after the ")" that closes the group just opened, or null */
    private String unitAfterGroup() {
        int depth = 1;
        for (int i = position; i < tokens.size(); i++) {
            if (Tokens.OPEN.equals(tokens.get(i))) depth++;
            else if (Tokens.CLOSE.equals(tokens.get(i)) && --depth == 0) {
                String next = i + 1 < tokens.size() ? tokens.get(i + 1) : null;
                return Convert.isUnit(next) ? next : null;
            }
        }
        return null;
    }

    private static Value inUnit(double number, String unit) {
        return new Value(Convert.x_y(number, unit, Convert.INCHES), true, unit);
    }

    private Value toLength(Value value) {
        if (value.isLength) return value;
        return inUnit(value.number, value.unit != null ? value.unit : defaultUnit);
    }

    private Value add(Value left, Value right, int sign) {
        if (!left.isLength && !right.isLength) {
            return new Value(left.number + sign * right.number, false, firstUnit(left, right));
        }
        Value a = toLength(left);
        Value b = toLength(right);
        return new Value(a.number + sign * b.number, true, sharedUnit(a, b));
    }

    private Value multiply(Value left, Value right) {
        if (!left.isLength && !right.isLength) return new Value(left.number * right.number, false, firstUnit(left, right));
        if (!left.isLength) return new Value(left.number * right.number, true, right.unit);
        if (!right.isLength) return new Value(left.number * right.number, true, left.unit);
        // Length × length multiplies the inches
        return new Value(left.number * right.number, true, sharedUnit(left, right));
    }

    private Value divide(Value left, Value right) {
        if (!right.isLength) return new Value(left.number / right.number, left.isLength, firstUnit(left, right));
        // Length ÷ length is how many fit, kept in the first length's unit
        Value a = toLength(left);
        return new Value(a.number / right.number, false, firstUnit(a, right));
    }

    private static String firstUnit(Value a, Value b) {
        return a.unit != null ? a.unit : b.unit;
    }

    private static String sharedUnit(Value a, Value b) {
        return a.unit != null && a.unit.equals(b.unit) ? a.unit : null;
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
