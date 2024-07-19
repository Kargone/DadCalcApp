package com.example.dadcalc;

import static androidx.constraintlayout.widget.StateSet.TAG;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private int currentFractionIndex = 0;
    public static final String[][] FRACTION_TYPES = { {"2", "4", "8"}, {"16", "32", "64"} };
    private boolean doFractionOutput = false;
    private int roundTo = 16;
    private boolean roundOutput = false;
    private String unitOutputType = "in";
    private boolean doingOperationStreak = false;
    public static final String[] OPERATION_TYPES = { "-", "+", "*", "/" };
    public Map<String, Integer> doubleTaps = new HashMap<>(Map.of(
            "input", 0,
            "output", 0,
            "operation", 0,
            "round", 0
    ));
    private double operationEqualsMemory = 0;
    public List<String> inputObject = new ArrayList<>();
    // May be a possible problem with firebase here and empty objects
    public List<List<String>> operationHistory = new ArrayList<>();
    private static final String CURSOR = "|";
    private String selectedScreen = "input-screen";

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        Double myValue = new Convert().x_y(10, "in", "mm");

        addStartingCursor();
        displayAndActivateNumKeys();
        displayAndActivateOperations();
        displayAndActivateArrowKeys();
        displayAndActivateUnitSelectors();
        displayAndActivateParentheses();
        displayAndActivateHistory();
        displayAndActivateFractionSelectors();
        displayAndActivateFraction();
        displayAndActivateDecimal();
        displayAndActivateDelete();
        displayAndActivateRound();
        displayAndActivateEquals();
    }
    private void addStartingCursor() {
        inputObject.add(CURSOR);
    }
    private void displayAndActivateNumKeys() {
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
    private void displayAndActivateOperations() {
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
            operationObject.setOnClickListener(v -> {
                setDoingOperationStreak(false);
                if (!Objects.equals(finalOperationSymbol, "/")) doubleTaps.put("operation", Arrays.toString(OPERATION_SYMBOLS).indexOf(finalOperationSymbol));
                else { resetDoubleTaps(); }

                setOperationEqualsMemory(evaluate(inputObject));
                int cursorPosition = inputObject.indexOf(CURSOR);
                if (cursorPosition == -1) throw new RuntimeException("Cursor not found");
                inputObject.add(cursorPosition, finalOperationSymbol);

                setSelectedScreen("input-screen");
                updateInputScreen();
            });
        }
    }
    private void displayAndActivateArrowKeys() {
        TextView leftArrowObject = setTextAndGetTextView("key_left_arrow", "←");
        leftArrowObject.setOnClickListener(v -> {
            resetDoubleTaps();
            int cursorPosition = inputObject.indexOf(CURSOR);
            if (cursorPosition == -1) throw new RuntimeException("Cursor not found");
            Log.d(TAG, "onClick: " + cursorPosition);
            if (cursorPosition == 0) return;
            inputObject.remove(cursorPosition);
            inputObject.add(cursorPosition - 1, CURSOR);
            setSelectedScreen("input-screen");
            updateInputScreen();
        });

        TextView rightArrowObject = setTextAndGetTextView("key_right_arrow", "→");
        rightArrowObject.setOnClickListener(v -> {
            resetDoubleTaps();
            int cursorPosition = inputObject.indexOf(CURSOR);
            if (cursorPosition == -1) throw new RuntimeException("Cursor not found");
            if (cursorPosition >= inputObject.size() - 1) return;
            inputObject.remove(cursorPosition);
            inputObject.add(cursorPosition + 1, CURSOR);
            setSelectedScreen("input-screen");
            updateInputScreen();
        });
    }
    private void displayAndActivateUnitSelectors() {
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
    private void displayAndActivateParentheses() {
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
    private void displayAndActivateHistory() {
        // TODO: Finish
        setTextAndGetTextView("key_history", "HISTORY");
    }
    private void displayAndActivateFractionSelectors() {
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
                    inputObject.add(cursorPosition, "/");
                }
            });
        }
    }
    private void displayAndActivateFraction() {
        TextView fractionKeyObject = setTextAndGetTextView("key_fraction", "FRAC");

        fractionKeyObject.setOnClickListener(v -> {
            if (Objects.equals(getSelectedScreen(), "input-screen")) {
                setCurrentFractionIndex((getCurrentFractionIndex() + 1) % 2);
                displayAndActivateFractionSelectors();
            } else if (Objects.equals(getSelectedScreen(), "output-screen")) {
                doFractionOutput = !doFractionOutput;
            } else {
                throw new RuntimeException("Not valid selected screen: " + getSelectedScreen());
            }
        });
    }
    private void displayAndActivateDecimal() {
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
    private void displayAndActivateDelete() {
        TextView deleteObject = setTextAndGetTextView("key_delete", "DEL");
        deleteObject.setOnClickListener(v -> {
            if (inputObject.size() <= 1) return;
            int cursorPosition = inputObject.indexOf(CURSOR);
            if (cursorPosition == -1) throw new RuntimeException("Cursor not found");

            inputObject.remove(cursorPosition - 1);
            setSelectedScreen("input-screen");
            updateInputScreen();
        });
    }
    private void displayAndActivateRound() {
        TextView roundObject = setTextAndGetTextView("key_round", "ROUND-16");
        roundObject.setOnClickListener(new View.OnClickListener() {
            /** @noinspection DataFlowIssue*/
            @Override
            public void onClick(View v) {
                roundOutput = !roundOutput;
                if (roundOutput) doubleTaps.put("round", doubleTaps.get("round") + 1);
                setRoundTo(16);
                resetDoubleTaps("round");
                updateInputScreen();
            }
        });
    }
    private void displayAndActivateEquals() {
        // TODO: Finish
        TextView equalObject = setTextAndGetTextView("key_equal", "=");
        equalObject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (doubleTaps.get("operation") != 0) {
                    if (!getDoingOperationStreak()) {
                        int cursorPosition = inputObject.indexOf(CURSOR);
                        if (cursorPosition == -1) throw new RuntimeException("Cursor not found");
                        inputObject.remove(cursorPosition - 1);
                    }
                    setDoingOperationStreak(true);

                    if (doubleTaps.get("operation") > 4) throw new RuntimeException("Invalid operation: " + doubleTaps.get("operation"));
                    String doOperation = OPERATION_TYPES[doubleTaps.get("operation")];

                    executeOperation(doOperation);
                    updateInputScreen();
                } else {
                    if (inputObject.size() <= 1) return;
                    operationHistory.add(inputObject);
                    if (operationHistory.size() > 10) operationHistory.remove(0);
                }
            }
            private void executeOperation(String operationType) {
                double evaluatedInputObject = evaluate(inputObject);
                switch (operationType) {
                    case "+":
                        evaluatedInputObject += getOperationEqualsMemory();
                        break;
                    case "-":
                        evaluatedInputObject -= getOperationEqualsMemory();
                        break;
                    case "*":
                        evaluatedInputObject *= getOperationEqualsMemory();
                        break;
                    case "/":
                        evaluatedInputObject /= getOperationEqualsMemory();
                        break;
                    default:
                        throw new RuntimeException("Invalid operation type: " + operationType);
                }
                String newInputObject = String.valueOf(roundTo7th(evaluatedInputObject)).concat(CURSOR);
                inputObject = convertToInputList(newInputObject);
            }
        });
    }
    private TextView setTextAndGetTextView(String ID, String text) {
        TextView textViewObject = getTextView(ID);
        textViewObject.setText(text);
        return textViewObject;
    }
    private TextView getTextView(String ID) {
        @SuppressLint("DiscouragedApi") int resID = getResources().getIdentifier(ID, "id", getPackageName());
        return findViewById(resID);
    }
    private void addTextToInputField(String text) {
        TextView textViewObject = getTextView("input_field");
        CharSequence oldText = textViewObject.getText();
        String newText = oldText + text;
        textViewObject.setText(newText);
    }
    public void updateInputScreen() {
        TextView roundObject = findViewById(R.id.key_round);
        String roundObjectText = "ROUND-" + getRoundTo();
        roundObject.setText(roundObjectText);
        setTextAndGetTextView("input_field", "");
        Log.d(TAG, "inputobj: " + inputObject);
        for (String character : inputObject) {
            addTextToInputField(character);
        }
        double output = evaluate(inputObject);
        setTextAndGetTextView("output_field", String.valueOf(output));
    }
    private void resetDoubleTaps(String except) {
        setDoingOperationStreak(false);
        for (String doubleTapsKey : doubleTaps.keySet()) {
            if (Objects.equals(doubleTapsKey, except)) continue;
            doubleTaps.put(doubleTapsKey, 0);

        }
    }
    private void resetDoubleTaps() {
        resetDoubleTaps("");
    }
    // TODO: make into class
    private double evaluate(List<String> inputObject) {
        return 0;
    }
    // TODO: finish
    private List<String> convertToInputList(String input) {
        return inputObject;
    }
    private double roundTo7th(double value) {
        return (double) Math.round(value * 10000000) / 10000000;
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
    private void setOperationEqualsMemory(double x) {
        operationEqualsMemory = x;
    }
    private double getOperationEqualsMemory() {
        return operationEqualsMemory;
    }
    private void setSelectedScreen(String x) {
        selectedScreen = x;
    }
    private String getSelectedScreen() {
        return selectedScreen;
    }
}