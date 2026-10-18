package com.karson.dadcalc;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.annotation.SuppressLint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Layout;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final String PREFERENCES_NAME = "dadcalc";
    private static final String STATE_KEY = "state";
    public static final String[][] FRACTION_TYPES = { {"2", "4", "8"}, {"16", "32", "64"} };
    private static final int[] FRACTION_KEY_IDS = { R.id.key_fraction_1, R.id.key_fraction_2, R.id.key_fraction_3 };
    private static final int DEFAULT_ROUND_TO = 16;
    private static final int MAX_HISTORY = 10;
    private static final String INPUT_SCREEN = "input-screen";
    private static final String OUTPUT_SCREEN = "output-screen";

    // Saved between launches
    private final List<String> inputObject = new ArrayList<>();
    private final List<List<String>> operationHistory = new ArrayList<>();
    private int currentFractionIndex = 0;
    private String unitOutputType = Convert.INCHES;
    private boolean doFractionOutput = false;
    private boolean roundOutput = false;
    private int roundTo = DEFAULT_ROUND_TO;
    private String selectedScreen = INPUT_SCREEN;

    // Double tap state, cleared by resetDoubleTaps()
    /** Operator just typed. Pressing = right away repeats it using {@link #operationEqualsMemory}. */
    private String repeatOperation = null;
    private double operationEqualsMemory = 0;
    private boolean doingOperationStreak = false;
    /** Right after ROUND is turned on, the fraction keys pick what to round to. */
    private boolean pickingRoundTo = false;
    private float inputTouchX = 0;
    private float inputTouchY = 0;

    private MaterialCardView inputCard;
    private MaterialCardView outputCard;
    private TextView inputField;
    private TextView outputField;
    private TextView outputLabel;
    private TextView fractionKey;
    private TextView roundKey;
    private final TextView[] fractionKeys = new TextView[FRACTION_KEY_IDS.length];
    private final Map<String, TextView> unitKeys = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        applySystemBarPadding();

        inputCard = findViewById(R.id.input_card);
        outputCard = findViewById(R.id.output_card);
        inputField = findViewById(R.id.input_field);
        outputField = findViewById(R.id.output_field);
        outputLabel = findViewById(R.id.output_label);

        loadState();

        displayAndActivateScreens();
        displayAndActivateNumKeys();
        displayAndActivateOperations();
        displayAndActivateArrowKeys();
        displayAndActivateUnitSelectors();
        displayAndActivateParentheses();
        displayAndActivateHistory();
        displayAndActivateFractionSelectors();
        displayAndActivateFraction();
        displayAndActivateDelete();
        displayAndActivateRound();
        displayAndActivateEquals();
        updateScreens();
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveState();
    }

    /** From Android 15 the app always draws behind the status and navigation bars, so pad the layout clear of them. */
    private void applySystemBarPadding() {
        View root = findViewById(R.id.root);
        int basePadding = root.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(basePadding + insets.left, basePadding + insets.top, basePadding + insets.right, basePadding + insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void displayAndActivateScreens() {
        inputCard.setOnClickListener(v -> {
            resetDoubleTaps();
            selectedScreen = INPUT_SCREEN;
            updateScreens();
        });
        // Remember where the input text was touched so the click can put the cursor there
        inputField.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                inputTouchX = event.getX();
                inputTouchY = event.getY();
            }
            return false;
        });
        inputField.setOnClickListener(v -> {
            resetDoubleTaps();
            Layout layout = inputField.getLayout();
            if (layout != null) {
                int line = layout.getLineForVertical((int) inputTouchY - inputField.getTotalPaddingTop() + inputField.getScrollY());
                float x = inputTouchX - inputField.getTotalPaddingLeft() + inputField.getScrollX();
                Tokens.moveCursorToTextOffset(inputObject, layout.getOffsetForHorizontal(line, x));
            }
            selectedScreen = INPUT_SCREEN;
            updateScreens();
        });
        outputCard.setOnClickListener(v -> {
            resetDoubleTaps();
            selectedScreen = OUTPUT_SCREEN;
            updateScreens();
        });
    }

    private void displayAndActivateNumKeys() {
        int[] numKeyIds = {
                R.id.key_0, R.id.key_1, R.id.key_2, R.id.key_3, R.id.key_4,
                R.id.key_5, R.id.key_6, R.id.key_7, R.id.key_8, R.id.key_9
        };
        for (int keyNumber = 0; keyNumber < numKeyIds.length; keyNumber++) {
            final String number = String.valueOf(keyNumber);
            findViewById(numKeyIds[keyNumber]).setOnClickListener(v -> typeIntoInput(number));
        }
        findViewById(R.id.key_decimal).setOnClickListener(v -> typeIntoInput(Tokens.DECIMAL));
    }

    private void displayAndActivateOperations() {
        activateOperation(R.id.key_add, Tokens.ADD);
        activateOperation(R.id.key_subtract, Tokens.SUBTRACT);
        activateOperation(R.id.key_multiply, Tokens.MULTIPLY);
        activateOperation(R.id.key_divide, Tokens.DIVIDE);
    }

    private void activateOperation(int keyId, String operation) {
        findViewById(keyId).setOnClickListener(v -> {
            resetDoubleTaps();
            try {
                operationEqualsMemory = Evaluator.evaluate(inputObject, unitOutputType);
                repeatOperation = operation;
            } catch (IllegalArgumentException e) {
                repeatOperation = null;
            }
            insertAtCursor(operation);
            selectedScreen = INPUT_SCREEN;
            updateScreens();
        });
    }

    private void displayAndActivateArrowKeys() {
        findViewById(R.id.key_left_arrow).setOnClickListener(v -> moveCursor(-1));
        findViewById(R.id.key_right_arrow).setOnClickListener(v -> moveCursor(1));
    }

    private void displayAndActivateUnitSelectors() {
        unitKeys.put(Convert.INCHES, (TextView) findViewById(R.id.key_inch_selector));
        unitKeys.put(Convert.FEET, (TextView) findViewById(R.id.key_feet_selector));
        unitKeys.put(Convert.MILLIMETERS, (TextView) findViewById(R.id.key_millimeter_selector));

        for (Map.Entry<String, TextView> unitKey : unitKeys.entrySet()) {
            final String unitSymbol = unitKey.getKey();
            unitKey.getValue().setOnClickListener(v -> {
                resetDoubleTaps();
                // With the output screen selected the unit keys change the output unit instead of typing
                if (OUTPUT_SCREEN.equals(selectedScreen)) unitOutputType = unitSymbol;
                else insertAtCursor(unitSymbol);
                updateScreens();
            });
        }
    }

    private void displayAndActivateParentheses() {
        findViewById(R.id.key_left_parenthesis).setOnClickListener(v -> typeIntoInput(Tokens.OPEN));
        findViewById(R.id.key_right_parenthesis).setOnClickListener(v -> typeIntoInput(Tokens.CLOSE));
    }

    private void displayAndActivateHistory() {
        findViewById(R.id.key_history).setOnClickListener(v -> {
            resetDoubleTaps();
            showHistory();
        });
    }

    private void showHistory() {
        if (operationHistory.isEmpty()) {
            Toast.makeText(this, R.string.history_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        List<String> labels = new ArrayList<>();
        List<Double> results = new ArrayList<>();
        for (int index = operationHistory.size() - 1; index >= 0; index--) {
            List<String> entry = operationHistory.get(index);
            Double result = null;
            String resultText;
            try {
                result = Evaluator.evaluate(entry, unitOutputType);
                resultText = formatOutput(result);
            } catch (IllegalArgumentException e) {
                resultText = getString(R.string.error);
            }
            labels.add(Tokens.render(entry) + "  =  " + resultText);
            results.add(result);
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.history_title)
                .setItems(labels.toArray(new String[0]), (dialog, which) -> {
                    Double result = results.get(which);
                    if (result == null) return;
                    resetDoubleTaps();
                    for (String token : Tokens.fromValue(result, unitOutputType)) insertAtCursor(token);
                    selectedScreen = INPUT_SCREEN;
                    updateScreens();
                })
                .setNeutralButton(R.string.clear_history, (dialog, which) -> operationHistory.clear())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void displayAndActivateFractionSelectors() {
        for (int fractionTypeIndex = 0; fractionTypeIndex < FRACTION_KEY_IDS.length; fractionTypeIndex++) {
            final int finalFractionTypeIndex = fractionTypeIndex;
            fractionKeys[fractionTypeIndex] = findViewById(FRACTION_KEY_IDS[fractionTypeIndex]);
            fractionKeys[fractionTypeIndex].setOnClickListener(v -> {
                String fractionDenominator = FRACTION_TYPES[currentFractionIndex][finalFractionTypeIndex];
                if (pickingRoundTo) {
                    roundTo = Integer.parseInt(fractionDenominator);
                    updateScreens();
                    return;
                }
                resetDoubleTaps();
                // With no numerator typed, the "1/16" key means one sixteenth
                int cursorPosition = getCursorPosition();
                if (cursorPosition == 0 || !Tokens.isDigit(inputObject.get(cursorPosition - 1))) insertAtCursor("1");
                insertAtCursor(Tokens.FRACTION_BAR);
                for (char digit : fractionDenominator.toCharArray()) insertAtCursor(String.valueOf(digit));
                selectedScreen = INPUT_SCREEN;
                updateScreens();
            });
        }
    }

    private void displayAndActivateFraction() {
        fractionKey = findViewById(R.id.key_fraction);
        fractionKey.setOnClickListener(v -> {
            boolean wasPickingRoundTo = pickingRoundTo;
            resetDoubleTaps();
            if (OUTPUT_SCREEN.equals(selectedScreen)) {
                doFractionOutput = !doFractionOutput;
            } else {
                currentFractionIndex = (currentFractionIndex + 1) % FRACTION_TYPES.length;
                // Lets ROUND, FRAC, 1/32 pick a rounding that isn't on the first page
                pickingRoundTo = wasPickingRoundTo;
            }
            updateScreens();
        });
    }

    private void displayAndActivateDelete() {
        TextView deleteKey = findViewById(R.id.key_delete);
        deleteKey.setOnClickListener(v -> {
            resetDoubleTaps();
            int cursorPosition = getCursorPosition();
            if (cursorPosition == 0) return;
            inputObject.remove(cursorPosition - 1);
            selectedScreen = INPUT_SCREEN;
            updateScreens();
        });
        deleteKey.setOnLongClickListener(v -> {
            resetDoubleTaps();
            clearInput();
            return true;
        });
    }

    private void displayAndActivateRound() {
        roundKey = findViewById(R.id.key_round);
        roundKey.setOnClickListener(v -> {
            resetDoubleTaps();
            roundOutput = !roundOutput;
            roundTo = DEFAULT_ROUND_TO;
            pickingRoundTo = roundOutput;
            updateScreens();
        });
    }

    private void displayAndActivateEquals() {
        findViewById(R.id.key_equal).setOnClickListener(v -> {
            if (repeatOperation == null) {
                // The output is always live, so a plain = just saves the calculation
                if (addToHistory(inputObject)) Toast.makeText(this, R.string.saved_to_history, Toast.LENGTH_SHORT).show();
                return;
            }
            if (!doingOperationStreak) {
                // Drop the operator that was just typed, so "5 +" becomes "5"
                int cursorPosition = getCursorPosition();
                if (cursorPosition > 0 && Tokens.isOperator(inputObject.get(cursorPosition - 1))) {
                    inputObject.remove(cursorPosition - 1);
                }
                doingOperationStreak = true;
            }
            try {
                double current = Evaluator.evaluate(inputObject, unitOutputType);
                double result = executeOperation(repeatOperation, current, operationEqualsMemory);
                List<String> resultTokens = Tokens.fromValue(result, unitOutputType);
                inputObject.clear();
                inputObject.addAll(resultTokens);
                inputObject.add(Tokens.CURSOR);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Could not repeat operation", e);
            }
            selectedScreen = INPUT_SCREEN;
            updateScreens();
        });
    }

    private static double executeOperation(String operation, double left, double right) {
        switch (operation) {
            case Tokens.ADD:
                return left + right;
            case Tokens.SUBTRACT:
                return left - right;
            case Tokens.MULTIPLY:
                return left * right;
            case Tokens.DIVIDE:
                return left / right;
            default:
                throw new IllegalArgumentException("Invalid operation type: " + operation);
        }
    }

    private void typeIntoInput(String token) {
        resetDoubleTaps();
        insertAtCursor(token);
        selectedScreen = INPUT_SCREEN;
        updateScreens();
    }

    private void insertAtCursor(String token) {
        inputObject.add(getCursorPosition(), token);
    }

    private void moveCursor(int offset) {
        resetDoubleTaps();
        int cursorPosition = getCursorPosition();
        int newPosition = cursorPosition + offset;
        if (newPosition < 0 || newPosition >= inputObject.size()) return;
        inputObject.remove(cursorPosition);
        inputObject.add(newPosition, Tokens.CURSOR);
        selectedScreen = INPUT_SCREEN;
        updateScreens();
    }

    private int getCursorPosition() {
        int cursorPosition = inputObject.indexOf(Tokens.CURSOR);
        if (cursorPosition != -1) return cursorPosition;
        Log.w(TAG, "Cursor missing, adding it back at the end");
        inputObject.add(Tokens.CURSOR);
        return inputObject.size() - 1;
    }

    private void clearInput() {
        addToHistory(inputObject);
        inputObject.clear();
        inputObject.add(Tokens.CURSOR);
        selectedScreen = INPUT_SCREEN;
        updateScreens();
    }

    /** @return false if there was nothing to save */
    private boolean addToHistory(List<String> input) {
        List<String> entry = new ArrayList<>(input);
        entry.remove(Tokens.CURSOR);
        if (entry.isEmpty()) return false;
        boolean isRepeat = !operationHistory.isEmpty() && operationHistory.get(operationHistory.size() - 1).equals(entry);
        if (!isRepeat) operationHistory.add(entry);
        if (operationHistory.size() > MAX_HISTORY) operationHistory.remove(0);
        return true;
    }

    private void resetDoubleTaps() {
        repeatOperation = null;
        doingOperationStreak = false;
        pickingRoundTo = false;
    }

    private String formatOutput(double value) {
        return OutputFormatter.format(value, unitOutputType, doFractionOutput, roundOutput, roundTo);
    }

    public void updateScreens() {
        boolean outputSelected = OUTPUT_SCREEN.equals(selectedScreen);
        int selectedStroke = ContextCompat.getColor(this, R.color.display_stroke_selected);
        int normalStroke = ContextCompat.getColor(this, R.color.display_stroke);
        inputCard.setStrokeColor(outputSelected ? normalStroke : selectedStroke);
        outputCard.setStrokeColor(outputSelected ? selectedStroke : normalStroke);
        outputLabel.setText(outputSelected ? R.string.output_label_selected : R.string.output_label);

        String inputText = Tokens.render(inputObject);
        SpannableString styledInput = new SpannableString(inputText);
        int cursorIndex = inputText.indexOf(Tokens.CURSOR);
        if (cursorIndex != -1) {
            styledInput.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.cursor)),
                    cursorIndex, cursorIndex + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            styledInput.setSpan(new StyleSpan(Typeface.BOLD), cursorIndex, cursorIndex + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        inputField.setText(styledInput);

        try {
            outputField.setText(formatOutput(Evaluator.evaluate(inputObject, unitOutputType)));
        } catch (IllegalArgumentException e) {
            outputField.setText(R.string.error);
        }

        for (Map.Entry<String, TextView> unitKey : unitKeys.entrySet()) {
            unitKey.getValue().setActivated(outputSelected && unitKey.getKey().equals(unitOutputType));
        }
        for (int fractionTypeIndex = 0; fractionTypeIndex < fractionKeys.length; fractionTypeIndex++) {
            String fractionDenominator = FRACTION_TYPES[currentFractionIndex][fractionTypeIndex];
            fractionKeys[fractionTypeIndex].setText(getString(R.string.key_fraction_slot, fractionDenominator));
            fractionKeys[fractionTypeIndex].setActivated(pickingRoundTo && Integer.parseInt(fractionDenominator) == roundTo);
        }
        fractionKey.setActivated(doFractionOutput);
        roundKey.setActivated(roundOutput);
        // Only "ROUND\n1/16" needs two lines; allowing two for plain "ROUND" lets autosize split the word
        roundKey.setMaxLines(roundOutput ? 2 : 1);
        roundKey.setText(roundOutput ? getString(R.string.key_round_to, roundTo) : getString(R.string.key_round));
    }

    private void saveState() {
        try {
            JSONArray history = new JSONArray();
            for (List<String> entry : operationHistory) history.put(new JSONArray(entry));
            JSONObject state = new JSONObject()
                    .put("input", new JSONArray(inputObject))
                    .put("history", history)
                    .put("currentFractionIndex", currentFractionIndex)
                    .put("unitOutputType", unitOutputType)
                    .put("doFractionOutput", doFractionOutput)
                    .put("roundOutput", roundOutput)
                    .put("roundTo", roundTo)
                    .put("selectedScreen", selectedScreen);
            getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE).edit().putString(STATE_KEY, state.toString()).apply();
        } catch (JSONException e) {
            Log.w(TAG, "Could not save state", e);
        }
    }

    private void loadState() {
        String savedState = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE).getString(STATE_KEY, null);
        if (savedState != null) {
            try {
                JSONObject state = new JSONObject(savedState);
                inputObject.addAll(toStringList(state.getJSONArray("input")));
                JSONArray history = state.getJSONArray("history");
                for (int index = 0; index < history.length(); index++) {
                    operationHistory.add(toStringList(history.getJSONArray(index)));
                }
                int savedFractionIndex = state.optInt("currentFractionIndex", 0);
                currentFractionIndex = savedFractionIndex >= 0 && savedFractionIndex < FRACTION_TYPES.length ? savedFractionIndex : 0;
                String savedUnit = state.optString("unitOutputType", Convert.INCHES);
                unitOutputType = Convert.isUnit(savedUnit) ? savedUnit : Convert.INCHES;
                doFractionOutput = state.optBoolean("doFractionOutput", false);
                roundOutput = state.optBoolean("roundOutput", false);
                roundTo = Math.max(1, state.optInt("roundTo", DEFAULT_ROUND_TO));
                selectedScreen = OUTPUT_SCREEN.equals(state.optString("selectedScreen")) ? OUTPUT_SCREEN : INPUT_SCREEN;
            } catch (JSONException e) {
                Log.w(TAG, "Ignoring unreadable saved state", e);
                inputObject.clear();
                operationHistory.clear();
            }
        }
        // Every key handler relies on there being exactly one cursor
        if (Collections.frequency(inputObject, Tokens.CURSOR) != 1) {
            inputObject.removeAll(Collections.singleton(Tokens.CURSOR));
            inputObject.add(Tokens.CURSOR);
        }
    }

    private static List<String> toStringList(JSONArray array) throws JSONException {
        List<String> list = new ArrayList<>();
        for (int index = 0; index < array.length(); index++) list.add(array.getString(index));
        return list;
    }
}
