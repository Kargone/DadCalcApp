package com.karson.dadcalc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tries every combination of operand type, operator and parenthesis placement in every output unit,
 * and checks the evaluator against answers worked out here step by step:
 * - a number without a unit is plain until it meets a length or is the answer, then it's inches,
 *   or the unit after its group, so "(2 × 3) ft" is 6 ft
 * - lengths are added, subtracted and multiplied as inches
 * - length ÷ length is how many fit, in the first length's unit, so "10 ft ÷ 2 ft" is 5 ft
 */
public class CalculationMatrixTest {
    private static final String F = Tokens.FRACTION_BAR;
    private static final String IN = Convert.INCHES;
    private static final String[] UNITS = {Convert.INCHES, Convert.FEET, Convert.MILLIMETERS};
    private static final String[] OPERATORS = {Tokens.ADD, Tokens.SUBTRACT, Tokens.MULTIPLY, Tokens.DIVIDE};

    /** A plain number, or a length in inches, with the unit it's in (null when none or mixed) */
    private static final class Answer {
        final double number;
        final boolean isLength;
        final String unit;

        Answer(double number, boolean isLength, String unit) {
            this.number = number;
            this.isLength = isLength;
            this.unit = unit;
        }

        static Answer plain(double number) {
            return new Answer(number, false, null);
        }

        static Answer length(double number, String unit) {
            return new Answer(Convert.x_y(number, unit, IN), true, unit);
        }

        Answer asLength(String defaultUnit) {
            return isLength ? this : length(number, unit != null ? unit : defaultUnit);
        }

        Answer negated() {
            return new Answer(-number, isLength, unit);
        }
    }

    /** An input operand: the tokens typed and what they mean */
    private static final class Operand {
        final String label;
        final List<String> tokens;
        final Answer answer;

        Operand(String label, Answer answer, String... tokens) {
            this.label = label;
            this.answer = answer;
            this.tokens = Arrays.asList(tokens);
        }
    }

    private static final Operand[] OPERANDS = {
            new Operand("3", Answer.plain(3), "3"),
            new Operand("3 in", Answer.length(3, "in"), "3", "in"),
            new Operand("2 ft", Answer.length(2, "ft"), "2", "ft"),
            new Operand("25.4 mm", Answer.length(25.4, "mm"), "2", "5", ".", "4", "mm"),
            new Operand("1/2", Answer.plain(0.5), "1", F, "2"),
            new Operand("3/8 in", Answer.length(0.375, "in"), "3", F, "8", "in"),
            new Operand("5 3/16 in", Answer.length(5.1875, "in"), "5", ".", "3", F, "1", "6", "in"),
            new Operand("1 ft 6 in", new Answer(18, true, null), "1", "ft", "6", "in"),
            new Operand("0.75 ft", Answer.length(0.75, "ft"), ".", "7", "5", "ft"),
            new Operand("12.7 mm", Answer.length(12.7, "mm"), "1", "2", ".", "7", "mm"),
    };

    private static String firstUnit(Answer a, Answer b) {
        return a.unit != null ? a.unit : b.unit;
    }

    private static String sharedUnit(Answer a, Answer b) {
        return a.unit != null && a.unit.equals(b.unit) ? a.unit : null;
    }

    /** One step of the calculation, with plain numbers taking defaultUnit when they become lengths */
    private static Answer apply(String operator, Answer a, Answer b, String defaultUnit) {
        boolean bothPlain = !a.isLength && !b.isLength;
        switch (operator) {
            case Tokens.ADD:
            case Tokens.SUBTRACT: {
                double sign = Tokens.ADD.equals(operator) ? 1 : -1;
                if (bothPlain) return new Answer(a.number + sign * b.number, false, firstUnit(a, b));
                Answer left = a.asLength(defaultUnit);
                Answer right = b.asLength(defaultUnit);
                return new Answer(left.number + sign * right.number, true, sharedUnit(left, right));
            }
            case Tokens.MULTIPLY:
                if (bothPlain) return new Answer(a.number * b.number, false, firstUnit(a, b));
                if (!a.isLength) return new Answer(a.number * b.number, true, b.unit);
                if (!b.isLength) return new Answer(a.number * b.number, true, a.unit);
                return new Answer(a.number * b.number, true, sharedUnit(a, b));
            case Tokens.DIVIDE: {
                if (!b.isLength) return new Answer(a.number / b.number, a.isLength, firstUnit(a, b));
                Answer left = a.asLength(defaultUnit);
                return new Answer(left.number / b.number, false, firstUnit(left, b));
            }
            default:
                throw new IllegalArgumentException(operator);
        }
    }

    private static Answer apply(String operator, Answer a, Answer b) {
        return apply(operator, a, b, IN);
    }

    /** What "(...) unit" means: plain answers are in that unit, lengths stay as they are */
    private static Answer withGroupUnit(Answer answer, String unit) {
        return answer.isLength ? answer : Answer.length(answer.number, unit);
    }

    private static boolean bindsTighter(String operator) {
        return Tokens.MULTIPLY.equals(operator) || Tokens.DIVIDE.equals(operator);
    }

    private int checked = 0;
    private final List<String> failures = new ArrayList<>();

    private void check(String description, List<String> input, Answer answer) {
        double expectedInches = answer.asLength(IN).number;
        if (Double.isNaN(expectedInches) || Double.isInfinite(expectedInches)) {
            checkDivisionByZero(description, input);
            return;
        }
        for (String unit : UNITS) {
            double expected = Convert.x_y(expectedInches, IN, unit);
            double actual;
            try {
                actual = Evaluator.evaluate(input, unit);
            } catch (IllegalArgumentException e) {
                failures.add(description + " [" + unit + "] threw " + e.getMessage());
                continue;
            }
            checked++;
            if (Math.abs(actual - expected) > 1e-9 * Math.max(1, Math.abs(expected))) {
                failures.add(description + " [" + unit + "] expected " + expected + " got " + actual);
            }
        }
    }

    /** Dividing by zero, such as 3 ÷ (2 ft − 2 ft), must show an error instead of an answer */
    private void checkDivisionByZero(String description, List<String> input) {
        for (String unit : UNITS) {
            checked++;
            try {
                failures.add(description + " [" + unit + "] gave " + Evaluator.evaluate(input, unit) + " instead of an error");
            } catch (IllegalArgumentException expected) {
                // Shown as an error, as it should be
            }
        }
    }

    private void assertNoFailures() {
        if (!failures.isEmpty()) {
            fail(failures.size() + " of " + (checked + failures.size()) + " checks failed, first ones:\n"
                    + String.join("\n", failures.subList(0, Math.min(20, failures.size()))));
        }
    }

    private static List<String> join(Object... parts) {
        List<String> tokens = new ArrayList<>();
        for (Object part : parts) {
            if (part instanceof Operand) tokens.addAll(((Operand) part).tokens);
            else tokens.add((String) part);
        }
        return tokens;
    }

    @Test
    public void everySingleOperationInEveryUnit() {
        for (Operand a : OPERANDS) {
            for (String op : OPERATORS) {
                for (Operand b : OPERANDS) {
                    check(a.label + " " + op + " " + b.label, join(a, op, b), apply(op, a.answer, b.answer));
                }
            }
        }
        assertNoFailures();
    }

    @Test
    public void everyPairOfOperationsWithAndWithoutParentheses() {
        for (Operand a : OPERANDS) {
            for (String op1 : OPERATORS) {
                for (Operand b : OPERANDS) {
                    for (String op2 : OPERATORS) {
                        for (Operand c : OPERANDS) {
                            String text = a.label + " " + op1 + " " + b.label + " " + op2 + " " + c.label;
                            Answer leftFirst = apply(op2, apply(op1, a.answer, b.answer), c.answer);
                            Answer rightFirst = apply(op1, a.answer, apply(op2, b.answer, c.answer));

                            // No parentheses: × and ÷ before + and −, otherwise left to right
                            boolean rightBindsFirst = bindsTighter(op2) && !bindsTighter(op1);
                            check(text, join(a, op1, b, op2, c), rightBindsFirst ? rightFirst : leftFirst);
                            check("(" + a.label + " " + op1 + " " + b.label + ") " + op2 + " " + c.label,
                                    join("(", a, op1, b, ")", op2, c), leftFirst);
                            check(a.label + " " + op1 + " (" + b.label + " " + op2 + " " + c.label + ")",
                                    join(a, op1, "(", b, op2, c, ")"), rightFirst);
                        }
                    }
                }
            }
        }
        assertNoFailures();
    }

    @Test
    public void unitAfterParenthesesAppliesToTheWholeGroup() {
        for (String groupUnit : UNITS) {
            for (Operand a : OPERANDS) {
                for (String op1 : OPERATORS) {
                    for (Operand b : OPERANDS) {
                        String group = "(" + a.label + " " + op1 + " " + b.label + ") " + groupUnit;
                        Answer inside = withGroupUnit(apply(op1, a.answer, b.answer, groupUnit), groupUnit);
                        check(group, join("(", a, op1, b, ")", groupUnit), inside);
                        check(group + " 6", join("(", a, op1, b, ")", groupUnit, "6"),
                                apply(Tokens.ADD, inside, Answer.length(6, IN)));
                        for (String op2 : OPERATORS) {
                            for (Operand c : new Operand[]{OPERANDS[0], OPERANDS[2], OPERANDS[4], OPERANDS[7]}) {
                                check(group + " " + op2 + " " + c.label, join("(", a, op1, b, ")", groupUnit, op2, c),
                                        apply(op2, inside, c.answer));
                                check(c.label + " " + op2 + " " + group, join(c, op2, "(", a, op1, b, ")", groupUnit),
                                        apply(op2, c.answer, inside));
                            }
                        }
                    }
                }
            }
        }
        assertNoFailures();
    }

    @Test
    public void negativesNestedParenthesesAndImplicitMultiply() {
        Answer two = Answer.plain(2);
        for (Operand a : OPERANDS) {
            for (String op : OPERATORS) {
                for (Operand b : OPERANDS) {
                    String text = a.label + " " + op + " " + b.label;
                    Answer group = apply(op, a.answer, b.answer);
                    check("−" + text, join(Tokens.SUBTRACT, a, op, b), apply(op, a.answer.negated(), b.answer));
                    check("−(" + text + ")", join(Tokens.SUBTRACT, "(", a, op, b, ")"), group.negated());
                    check("((" + text + "))", join("(", "(", a, op, b, ")", ")"), group);
                    check("2(" + text + ")", join("2", "(", a, op, b, ")"), apply(Tokens.MULTIPLY, two, group));
                    check("(" + text + ")2", join("(", a, op, b, ")", "2"), apply(Tokens.MULTIPLY, group, two));
                    check("(" + text + ")(" + text + ")", join("(", a, op, b, ")", "(", a, op, b, ")"),
                            apply(Tokens.MULTIPLY, group, group));
                }
            }
        }
        assertNoFailures();
    }

    @Test
    public void answerIsTheSameLengthInEveryOutputUnit() {
        for (Operand a : OPERANDS) {
            for (String op1 : OPERATORS) {
                for (Operand b : OPERANDS) {
                    for (String op2 : OPERATORS) {
                        List<String> input = join(a, op1, "(", b, op2, a, ")");
                        double inches;
                        try {
                            inches = Evaluator.evaluate(input, IN);
                        } catch (IllegalArgumentException divisionByZero) {
                            continue;
                        }
                        double tolerance = 1e-9 * Math.max(1, Math.abs(inches));
                        assertEquals(inches / 12, Evaluator.evaluate(input, Convert.FEET), tolerance);
                        assertEquals(inches * 25.4, Evaluator.evaluate(input, Convert.MILLIMETERS), tolerance);
                    }
                }
            }
        }
    }
}
