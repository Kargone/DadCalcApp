package com.karson.dadcalc;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * The input is a list of single tokens: digits, ".", operators, parentheses, units, the fraction
 * bar and exactly one cursor. Keeping one key press per token makes DEL and the arrow keys trivial.
 */
public final class Tokens {
    public static final String CURSOR = "|";
    public static final String DECIMAL = ".";
    /** Typed by the fraction keys. Binds tighter than divide, so "3⁄16 in" is three sixteenths of an inch. */
    public static final String FRACTION_BAR = "⁄";
    public static final String ADD = "+";
    public static final String SUBTRACT = "−";
    public static final String MULTIPLY = "×";
    public static final String DIVIDE = "÷";
    public static final String OPEN = "(";
    public static final String CLOSE = ")";

    private Tokens() {}

    public static boolean isDigit(String token) {
        return token != null && token.length() == 1 && Character.isDigit(token.charAt(0));
    }

    public static boolean isOperator(String token) {
        return ADD.equals(token) || SUBTRACT.equals(token) || MULTIPLY.equals(token) || DIVIDE.equals(token);
    }

    /** Turns a result back into tokens so it can be typed into the input, e.g. after a repeated operation. */
    public static List<String> fromValue(double value, String unit) {
        List<String> tokens = new ArrayList<>();
        BigDecimal magnitude = BigDecimal.valueOf(Math.abs(value)).setScale(7, RoundingMode.HALF_UP).stripTrailingZeros();
        if (value < 0 && magnitude.signum() != 0) tokens.add(SUBTRACT);
        for (char character : magnitude.toPlainString().toCharArray()) {
            tokens.add(String.valueOf(character));
        }
        tokens.add(unit);
        return tokens;
    }

    /** Text shown on the input screen. The cursor is kept as {@link #CURSOR}. */
    public static String render(List<String> tokens) {
        return renderWithPositions(tokens).text;
    }

    /** Rendered text plus where each token ended up in it, so a tap on the text can be mapped back to a token. */
    public static final class Rendered {
        public final String text;
        /** Character index each token's text starts at, not counting the spaces around it. */
        public final int[] tokenStarts;
        public final int[] tokenEnds;

        private Rendered(String text, int[] tokenStarts, int[] tokenEnds) {
            this.text = text;
            this.tokenStarts = tokenStarts;
            this.tokenEnds = tokenEnds;
        }
    }

    public static Rendered renderWithPositions(List<String> tokens) {
        StringBuilder text = new StringBuilder();
        int[] tokenStarts = new int[tokens.size()];
        int[] tokenEnds = new int[tokens.size()];
        String previous = null;
        for (int index = 0; index < tokens.size(); index++) {
            String token = tokens.get(index);
            String shown = token;
            boolean spaceBefore = false;
            boolean spaceAfter = false;
            if (CURSOR.equals(token)) {
                tokenStarts[index] = text.length();
                text.append(CURSOR);
                tokenEnds[index] = text.length();
                continue;
            }
            if (isOperator(token)) {
                boolean negativeSign = SUBTRACT.equals(token) && (previous == null || isOperator(previous) || OPEN.equals(previous));
                spaceBefore = !negativeSign;
                spaceAfter = !negativeSign;
            } else if (Convert.isUnit(token)) {
                spaceBefore = true;
            } else {
                spaceBefore = Convert.isUnit(previous);
                if (FRACTION_BAR.equals(token)) shown = "/";
                // "5.3⁄16" is shown as "5 3/16"
                else if (DECIMAL.equals(token) && isMixedNumberSeparator(tokens, index)) shown = " ";
            }
            if (spaceBefore) appendSpace(text);
            tokenStarts[index] = text.length();
            text.append(shown);
            tokenEnds[index] = text.length();
            if (spaceAfter) text.append(' ');
            previous = token;
        }
        return new Rendered(text.toString(), tokenStarts, tokenEnds);
    }

    /** Adds a space unless it would lead the text or double up. */
    private static void appendSpace(StringBuilder text) {
        int length = text.length();
        if (length == 0 || text.charAt(length - 1) == ' ') return;
        if (length == 1 && text.charAt(0) == CURSOR.charAt(0)) return;
        text.append(' ');
    }

    /**
     * Moves the cursor to the token gap closest to a character offset in the rendered text,
     * e.g. one from a tap on the input screen.
     */
    public static void moveCursorToTextOffset(List<String> tokens, int textOffset) {
        Rendered rendered = renderWithPositions(tokens);
        int newCursorPosition = 0;
        for (int index = 0; index < tokens.size(); index++) {
            if (CURSOR.equals(tokens.get(index))) continue;
            int middle = (rendered.tokenStarts[index] + rendered.tokenEnds[index] + 1) / 2;
            if (middle > textOffset) break;
            newCursorPosition++;
        }
        tokens.remove(CURSOR);
        tokens.add(newCursorPosition, CURSOR);
    }

    /** "5.3⁄16" is the mixed number 5 3/16, so its "." is shown as a space. */
    private static boolean isMixedNumberSeparator(List<String> tokens, int decimalIndex) {
        int index = decimalIndex + 1;
        while (index < tokens.size() && (isDigit(tokens.get(index)) || CURSOR.equals(tokens.get(index)))) index++;
        return index < tokens.size() && FRACTION_BAR.equals(tokens.get(index));
    }
}
