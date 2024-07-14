package com.example.dadcalc;

import static androidx.constraintlayout.widget.StateSet.TAG;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private int currentFractionIndex = 0;
    private static final String[][] FRACTION_TYPES = { {"2", "4", "8"}, {"16", "32", "64"} };
    private boolean doFractionOutput = false;
    private int roundTo = 16;
    private boolean roundOutput = false;
    private String unitOutputType = "in";
    private boolean doingOperationStreak = false;
    public HashMap<String, Integer> doubleTaps = new HashMap<>(Map.of(
            "input", 0,
            "output", 0,
            "operation", 0,
            "round", 0
    ));
    private int operationEqualsMemory = 0;
    public final List<String> inputObject = new ArrayList<>();
    private static final String CURSOR = "|";
    private static final String DECIMAL_START = ".";
    private static final String DENOMINATOR_START = "%";
    private static final String NUMERATOR_START = "$";
    private String selectedScreen = "input-screen";

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        addStartingCursor();
        Double myValue = new Convert().x_y(10, "in", "mm");

        displayNumKeys();
        displayOperations();
        displayArrowKeys();
        displayUnitSelectors();
        displayParentheses();
        displayHistory();
        displayFractionSelectors();
        displayFraction();
        displayDecimal();
        displayDelete();
        displayRound();
        displayEquals();
    }
    private void addStartingCursor() {
        inputObject.add(CURSOR);
    }
    private void displayNumKeys() {
        for (int i = 0; i < 10; i++) {
            final int keyNumber = i;
            String numKeyId = "key_" + keyNumber;
            TextView numKeyObject = setTextAndGetTextView(numKeyId, String.valueOf(keyNumber));
            numKeyObject.setOnClickListener(v -> {
                int cursorPosition = inputObject.indexOf(CURSOR);
                if (cursorPosition == -1) throw new RuntimeException("Cursor not found");
                inputObject.add(cursorPosition, String.valueOf(keyNumber));
                setSelectedScreen("input-screen");
                resetDoubleTaps();
                updateInputScreen();
            });
        }
    }
    private void displayOperations() {
        final String[] OPERATION_SYMBOLS = {"+", "-", "x", "/"};
        final Map<String, String> OPERATION_TYPES = Map.of(
                "+", "add",
                "-", "subtract",
                "x", "multiply",
                "/", "divide"
        );

        for (String operationSymbol : OPERATION_SYMBOLS) {
            final String finalOperationSymbol = operationSymbol;
            String operationType = OPERATION_TYPES.get(operationSymbol);
            String operationKeyId = "key_" + operationType;

            TextView operationObject = setTextAndGetTextView(operationKeyId, operationSymbol);
            // TODO: finish func
            operationObject.setOnClickListener(v -> {
                setDoingOperationStreak(false);
                // TODO: fix the line below with doubletaps
                if (!Objects.equals(finalOperationSymbol, "/")) doubleTaps.replace("operation", 1);
                    else resetDoubleTaps();
                setOperationEqualsMemory(evaluate(inputObject));

                int cursorPosition = inputObject.indexOf(CURSOR);
                if (cursorPosition == -1) throw new RuntimeException("Cursor not found");
                inputObject.add(cursorPosition, finalOperationSymbol);

                setSelectedScreen("input-screen");
                updateInputScreen();
            });
        }
    }
    private void displayArrowKeys() {
        TextView leftArrowObject = setTextAndGetTextView("key_left_arrow", "←");
        leftArrowObject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetDoubleTaps();
                int cursorPosition = inputObject.indexOf(CURSOR);
                if (cursorPosition == -1) throw new RuntimeException("Cursor not found");
                Log.d(TAG, "onClick: " + cursorPosition);
                if (cursorPosition == 0) return;
                inputObject.remove(cursorPosition);
                if (moveOverDenominator(cursorPosition)) {
                    inputObject.add(cursorPosition - 2, CURSOR);
                } else {
                    inputObject.add(cursorPosition - 1, CURSOR);
                }
                setSelectedScreen("input-screen");
                updateInputScreen();
            }
            private boolean moveOverDenominator(int cursorPosition) {
                if (cursorPosition <= 1) return false;
                return Objects.equals(inputObject.get(cursorPosition - 2), DENOMINATOR_START);
            }
        });


        TextView rightArrowObject = setTextAndGetTextView("key_right_arrow", "→");
        rightArrowObject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetDoubleTaps();
                int cursorPosition = inputObject.indexOf(CURSOR);
                if (cursorPosition == -1) throw new RuntimeException("Cursor not found");
                if (cursorPosition >= inputObject.size() - 1) return;
                inputObject.remove(cursorPosition);
                if (moveOverDenominator(cursorPosition)) {
                    inputObject.add(cursorPosition + 2, CURSOR);
                } else {
                    inputObject.add(cursorPosition + 1, CURSOR);
                }
                setSelectedScreen("input-screen");
                updateInputScreen();
            }
            private boolean moveOverDenominator(int cursorPosition) {
                if (cursorPosition >= inputObject.size() - 1) return false;
                return Objects.equals(inputObject.get(cursorPosition + 1), DENOMINATOR_START);
            }
        });
    }
    private void displayUnitSelectors() {
        final String[] UNIT_SELECTORS = {"inch", "feet", "millimeter"};
        final Map<String, String> UNIT_SYMBOLS = Map.of(
                "inch", "in",
                "feet", "ft",
                "millimeter", "mm"
        );

        for (String unitSelector : UNIT_SELECTORS) {
            String unitSelectorKeyId = "key_" + unitSelector + "_selector";
            final String unitSymbol = UNIT_SYMBOLS.get(unitSelector);

            TextView unitSelectorObject = setTextAndGetTextView(unitSelectorKeyId, unitSymbol);
            unitSelectorObject.setOnClickListener(v -> {
                resetDoubleTaps();
                if (Objects.equals(getSelectedScreen(), "input-screen")) {
                    int cursorPosition = inputObject.indexOf(CURSOR);
                    if (cursorPosition == -1) throw new RuntimeException("Cursor not found");
                    inputObject.add(cursorPosition, "(" + unitSymbol + ")");
                    updateInputScreen();
                } else if (Objects.equals(getSelectedScreen(), "output-screen")) {
                    setUnitOutputType(unitSymbol);
                    updateInputScreen();
                } else {
                    throw new RuntimeException("Not valid selected screen: " + getSelectedScreen());
                }
            });
        }
    }
    private void displayParentheses() {
        TextView leftParenthesisObject = setTextAndGetTextView("key_left_parenthesis", "(");
        leftParenthesisObject.setOnClickListener(v -> {
            resetDoubleTaps();
            int cursorPosition = inputObject.indexOf(CURSOR);
            if (cursorPosition == -1) throw new RuntimeException("Cursor not found");
            inputObject.add(cursorPosition, "(");
            updateInputScreen();
        });

        TextView rightParenthesisObject = setTextAndGetTextView("key_right_parenthesis", ")");
        rightParenthesisObject.setOnClickListener(v -> {
            int cursorPosition = inputObject.indexOf(CURSOR);
            if (cursorPosition == -1) throw new RuntimeException("Cursor not found");
            inputObject.add(cursorPosition, ")");
            updateInputScreen();
        });
    }
    private void displayHistory() {
        // TODO: Finish
        setTextAndGetTextView("key_history", "HISTORY");
    }
    private void displayFractionSelectors() {
        for (int fractionTypeIndex = 0; fractionTypeIndex < 3; fractionTypeIndex++) {
            final int finalFractionTypeIndex = fractionTypeIndex;
            String fractionDenominator = FRACTION_TYPES[currentFractionIndex][fractionTypeIndex];
            String currentFractionKeyId = "key_fraction_" + (fractionTypeIndex + 1);

            String fractionText = "1/" + fractionDenominator;
            TextView fractionObject = setTextAndGetTextView(currentFractionKeyId, fractionText);
            fractionObject.setOnClickListener(new View.OnClickListener() {
                /** @noinspection DataFlowIssue*/
                @Override
                public void onClick(View v) {
                    String fractionDenominator = FRACTION_TYPES[currentFractionIndex][finalFractionTypeIndex];
                    if (doubleTaps.get("round") > 0) {
                        setRoundTo(Integer.parseInt(fractionDenominator));
                        updateInputScreen();
                        return;
                    }
                    resetDoubleTaps();

                    int cursorPosition = inputObject.indexOf(CURSOR);
                    if (cursorPosition == -1) throw new RuntimeException("Cursor not found");
                    addFractionDenominatorToInputObject(fractionDenominator, cursorPosition);

                    updateInputScreen();
                }
                private void addFractionDenominatorToInputObject(String fractionDenominator, int cursorPosition) {
                    String[] reversedFractionDenominator = new StringBuilder(fractionDenominator).reverse().toString().split("");
                    for (String numberOfDenominator : reversedFractionDenominator) {
                        inputObject.add(cursorPosition, numberOfDenominator);
                    }
                    inputObject.add(cursorPosition, " / ");
                }
            });
        }
    }
    private void displayFraction() {
        TextView fractionKeyObject = setTextAndGetTextView("key_fraction", "FRAC");

        fractionKeyObject.setOnClickListener(v -> {
            if (Objects.equals(getSelectedScreen(), "input-screen")) {
                setCurrentFractionIndex((getCurrentFractionIndex() + 1) % 2);
                displayFractionSelectors();
            } else if (Objects.equals(getSelectedScreen(), "output-screen")) {
                doFractionOutput = !doFractionOutput;
            } else {
                throw new RuntimeException("Not valid selected screen: " + getSelectedScreen());
            }
        });
    }
    private void displayDecimal() {
        TextView decimalObject = setTextAndGetTextView("key_decimal", ".");
        decimalObject.setOnClickListener(v -> {
            int cursorPosition = inputObject.indexOf(CURSOR);
            if (cursorPosition == -1) throw new RuntimeException("Cursor not found");
            inputObject.add(cursorPosition, ".");
            setSelectedScreen("input-screen");
            resetDoubleTaps();
            updateInputScreen();
        });
    }
    private void displayDelete() {
        TextView deleteObject = setTextAndGetTextView("key_delete", "DEL");
        deleteObject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (inputObject.size() <= 1) return;
                int cursorPosition = inputObject.indexOf(CURSOR);
                if (cursorPosition == -1) throw new RuntimeException("Cursor not found");

                if (checkIfDenominator(cursorPosition)) return;
                if (checkIfNumerator(cursorPosition)) return;

                inputObject.remove(cursorPosition - 1);
                setSelectedScreen("input-screen");

                updateInputScreen();
            }
            // TODO: rename the below methods, and some variables
            private boolean checkIfDenominator(int cursorPosition) {
                if (cursorPosition < 2) return false;
                if (Objects.equals(inputObject.get(cursorPosition - 2), DENOMINATOR_START)) {
                    final List<String> inputObjectUpToTheCursor = inputObject.subList(0, cursorPosition);
                    int fractionStart = inputObjectUpToTheCursor.lastIndexOf(NUMERATOR_START);
                    if (fractionStart == -1) throw new RuntimeException("Denominator start not found");

                    inputObject.set(fractionStart, DECIMAL_START);
                    inputObject.remove(cursorPosition - 1);
                    inputObject.remove(cursorPosition - 2);

                    return true;
                }
                return false;
            }
            // TODO: finish func
            private boolean checkIfNumerator(int cursorPosition) {
                if (cursorPosition < 1) return false;
                return Objects.equals(inputObject.get(cursorPosition - 1), NUMERATOR_START);
            }
        });
    }
    private void displayRound() {
        TextView roundObject = setTextAndGetTextView("key_round", "ROUND-16");
        roundObject.setOnClickListener(new View.OnClickListener() {
            /** @noinspection DataFlowIssue*/
            @Override
            public void onClick(View v) {
                roundOutput = !roundOutput;
                if (roundOutput) doubleTaps.put("round", doubleTaps.get("round") + 1);
                setRoundTo(16);
                resetDoubleTaps();
                updateInputScreen();
            }
        });
    }
    private void displayEquals() {
        // TODO: Finish
        setTextAndGetTextView("key_equal", "=");
    }
    private TextView setTextAndGetTextView(String ID, String text) {
        @SuppressLint("DiscouragedApi") int resID = getResources().getIdentifier(ID, "id", getPackageName());
        TextView textViewObject = findViewById(resID);
        textViewObject.setText(text);
        return textViewObject;
    }
    private TextView getTextView(String ID) {
        @SuppressLint("DiscouragedApi") int resID = getResources().getIdentifier(ID, "id", getPackageName());
        return findViewById(resID);
    }
    private void addTextTo(String ID, String text) {
        @SuppressLint("DiscouragedApi") int resID = getResources().getIdentifier(ID, "id", getPackageName());
        TextView textViewObject = findViewById(resID);
        CharSequence oldText = textViewObject.getText();
        textViewObject.setText(oldText + text);
    }
    public void updateInputScreen() {
        TextView roundObject = findViewById(R.id.key_round);
        roundObject.setText("ROUND-" + getRoundTo());
        boolean fractionOpen = false;
        setTextAndGetTextView("input_field", "");
        Log.d(TAG, "inputobj: " + inputObject);
        for (String character : inputObject) {
            if (Objects.equals(character, "$")) {
                addTextTo("input_field", " (");
            } else if (Objects.equals(character, "%")) {
                addTextTo("input_field", " /");
                fractionOpen = true;
            } else {
                addTextTo("input_field", character);
                if (fractionOpen) {
                    fractionOpen = false;
                    addTextTo("input_field", " )");
                }
            }
        }
        int output = evaluate(inputObject);
        setTextAndGetTextView("output_field", String.valueOf(output));
    }
    // TODO: might need optional argument of type
    private void resetDoubleTaps() {

    }
    // TODO: make into class
    private int evaluate(List inputObject) {
        return 0;
    }
    private void setCurrentFractionIndex(int x) {
        currentFractionIndex = x;
    }
    private int getCurrentFractionIndex() {
        return currentFractionIndex;
    }
    private void setUnitOutputType(String x) {
        unitOutputType = x;
    }
    private String getUnitOutputType() {
        return unitOutputType;
    }
    private void setDoingOperationStreak(boolean x) {
        doingOperationStreak = x;
    }
    private boolean getDoingOperationStreak() {
        return doingOperationStreak;
    }
    private void setRoundTo(int x) {
        roundTo = x;
    }
    private int getRoundTo() {
        return roundTo;
    }
    private void setOperationEqualsMemory(int x) {
        operationEqualsMemory = x;
    }
    private int getOperationEqualsMemory() {
        return operationEqualsMemory;
    }
    private void setSelectedScreen(String x) {
        selectedScreen = x;
    }
    private String getSelectedScreen() {
        return selectedScreen;
    }
}