package com.example.kalkulator;

import android.content.Intent;
import android.os.Bundle;
import android.widget.HorizontalScrollView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

/**
 * Main calculator screen for CALQ.
 */
public class MainActivity extends AppCompatActivity {

    private static final String STATE_EXPRESSION = "state_expression";
    private static final String STATE_RESULT = "state_result";
    private static final String STATE_JUST_EVALUATED = "state_just_evaluated";

    private TextView tvExpression;
    private TextView tvResult;
    private HorizontalScrollView scrollExpression;

    private HistoryRepository historyRepository;

    private String expression = "0";
    private String lastCommittedResult = "";
    private boolean justEvaluated = false;

    private final ActivityResultLauncher<Intent> historyLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String selectedValue = result.getData()
                            .getStringExtra(IntentConstants.EXTRA_SELECTED_VALUE);
                    if (selectedValue != null && !selectedValue.isEmpty()) {
                        insertExternalValue(selectedValue);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        historyRepository = new HistoryRepository(this);

        tvExpression = findViewById(R.id.tvExpression);
        tvResult = findViewById(R.id.tvResult);
        scrollExpression = findViewById(R.id.scrollExpression);

        if (savedInstanceState != null) {
            expression = savedInstanceState.getString(STATE_EXPRESSION, "0");
            lastCommittedResult = savedInstanceState.getString(STATE_RESULT, "");
            justEvaluated = savedInstanceState.getBoolean(STATE_JUST_EVALUATED, false);
        } else if (getIntent() != null && getIntent().hasExtra(IntentConstants.EXTRA_SELECTED_VALUE)) {
            String initialVal = getIntent().getStringExtra(IntentConstants.EXTRA_SELECTED_VALUE);
            if (initialVal != null && !initialVal.isEmpty()) {
                insertExternalValue(initialVal);
            }
        }

        getSupportFragmentManager().setFragmentResultListener(
                QuickToolsBottomSheet.REQUEST_KEY,
                this,
                (requestKey, bundle) -> {
                    String value = bundle.getString(QuickToolsBottomSheet.RESULT_VALUE_KEY);
                    if (value != null && !value.isEmpty()) {
                        insertExternalValue(value);
                    }
                }
        );

        bindNavigationButtons();
        bindKeypadButtons();
        updateDisplay();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent != null && intent.hasExtra(IntentConstants.EXTRA_SELECTED_VALUE)) {
            String val = intent.getStringExtra(IntentConstants.EXTRA_SELECTED_VALUE);
            if (val != null && !val.isEmpty()) {
                insertExternalValue(val);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_EXPRESSION, expression);
        outState.putString(STATE_RESULT, lastCommittedResult);
        outState.putBoolean(STATE_JUST_EVALUATED, justEvaluated);
    }

    private void bindNavigationButtons() {
        MaterialButton btnHistory = findViewById(R.id.btnHistory);
        MaterialButton btnQuickTools = findViewById(R.id.btnQuickTools);

        btnHistory.setOnClickListener(v -> {
            Intent intent = new Intent(this, HistoryActivity.class);
            historyLauncher.launch(intent);
        });

        btnQuickTools.setOnClickListener(v -> {
            QuickToolsBottomSheet sheet = new QuickToolsBottomSheet();
            sheet.show(getSupportFragmentManager(), "QuickToolsBottomSheet");
        });
    }

    private void bindKeypadButtons() {
        int[] digitIds = {
                R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
                R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9
        };
        for (int i = 0; i < digitIds.length; i++) {
            final String digit = String.valueOf(i);
            findViewById(digitIds[i]).setOnClickListener(v -> appendDigit(digit));
        }

        findViewById(R.id.btnDot).setOnClickListener(v -> appendDot());
        findViewById(R.id.btnAdd).setOnClickListener(v -> appendOperator("+"));
        findViewById(R.id.btnSubtract).setOnClickListener(v -> appendOperator("−"));
        findViewById(R.id.btnMultiply).setOnClickListener(v -> appendOperator("×"));
        findViewById(R.id.btnDivide).setOnClickListener(v -> appendOperator("÷"));
        findViewById(R.id.btnPercent).setOnClickListener(v -> appendPercent());
        findViewById(R.id.btnToggleSign).setOnClickListener(v -> toggleSign());
        findViewById(R.id.btnClear).setOnClickListener(v -> clearAll());
        findViewById(R.id.btnBackspace).setOnClickListener(v -> backspace());
        findViewById(R.id.btnBackspace).setOnLongClickListener(v -> {
            clearAll();
            return true;
        });
        findViewById(R.id.btnEquals).setOnClickListener(v -> evaluateFinal());
    }
    private void appendDigit(String digit) {
        if (justEvaluated) {
            expression = digit;
            lastCommittedResult = "";
            justEvaluated = false;
        } else if ("0".equals(expression)) {
            expression = digit;
        } else if (expression.endsWith("%")) {
            expression += "×" + digit;
        } else {
            expression += digit;
        }
        updateDisplay();
    }

    private void appendDot() {
        if (justEvaluated) {
            expression = "0,";
            lastCommittedResult = "";
            justEvaluated = false;
            updateDisplay();
            return;
        }
        if (endsWithOperator(expression)) {
            expression += "0,";
            updateDisplay();
            return;
        }
        if (expression.endsWith("%")) {
            expression += "×0,";
            updateDisplay();
            return;
        }
        if (!currentNumberHasComma()) {
            expression += ",";
            updateDisplay();
        }
    }

    private void appendOperator(String op) {
        if (justEvaluated) {
            if (!lastCommittedResult.isEmpty()) {
                expression = lastCommittedResult + op;
            }
            justEvaluated = false;
            lastCommittedResult = "";
            updateDisplay();
            return;
        }
        if (expression.isEmpty()) {
            expression = "0" + op;
        } else if (endsWithOperator(expression)) {
            expression = expression.substring(0, expression.length() - 1) + op;
        } else {
            expression += op;
        }
        updateDisplay();
    }

    private void appendPercent() {
        if (justEvaluated && !lastCommittedResult.isEmpty()) {
            expression = lastCommittedResult + "%";
            justEvaluated = false;
            lastCommittedResult = "";
            updateDisplay();
            return;
        }
        if (!expression.isEmpty() && !endsWithOperator(expression) && !expression.endsWith("%")) {
            expression += "%";
            updateDisplay();
        }
    }

    private void toggleSign() {
        if (justEvaluated && !lastCommittedResult.isEmpty()) {
            expression = lastCommittedResult.startsWith("-")
                    ? lastCommittedResult.substring(1)
                    : "-" + lastCommittedResult;
            lastCommittedResult = "";
            justEvaluated = false;
            updateDisplay();
            return;
        }
        if ("0".equals(expression)) {
            return;
        }
        int opIdx = findLastBinaryOperatorIndex(expression);
        if (opIdx == -1) {
            expression = expression.startsWith("-") ? expression.substring(1) : "-" + expression;
        } else if (opIdx < expression.length() - 1) {
            String prefix = expression.substring(0, opIdx + 1);
            String tail = expression.substring(opIdx + 1);
            if (tail.startsWith("-")) {
                expression = prefix + tail.substring(1);
            } else {
                expression = prefix + "-" + tail;
            }
        }
        updateDisplay();
    }

    private void backspace() {
        if (justEvaluated) {
            justEvaluated = false;
            lastCommittedResult = "";
        }
        if (expression.length() <= 1 || (expression.length() == 2 && expression.startsWith("-"))) {
            expression = "0";
        } else {
            expression = expression.substring(0, expression.length() - 1);
        }
        updateDisplay();
    }

    private void clearAll() {
        expression = "0";
        lastCommittedResult = "";
        justEvaluated = false;
        updateDisplay();
    }
    private void evaluateFinal() {
        if (justEvaluated) {
            return;
        }
        String evalTarget = trimTrailingOperator(expression);
        if (evalTarget.isEmpty()) {
            return;
        }
        CalculatorEngine.EvaluationResult res = CalculatorEngine.evaluate(evalTarget);
        if (!res.success) {
            tvResult.setTextColor(getColor(R.color.red_error));
            tvResult.setText(res.errorMessage);
            justEvaluated = false;
            return;
        }

        lastCommittedResult = res.formattedResult;
        justEvaluated = true;
        tvExpression.setText(CalculatorEngine.formatExpressionForDisplay(evalTarget));
        tvResult.setTextColor(getColor(R.color.accent_orange));
        tvResult.setText(res.formattedResult);

        if (containsOperation(evalTarget)) {
            historyRepository.add(new HistoryItem(
                    CalculatorEngine.formatExpressionForDisplay(evalTarget),
                    res.formattedResult,
                    HistoryItem.CATEGORY_CALCULATOR
            ));
        }
    }

    private void insertExternalValue(String value) {
        String sanitized = value.trim();
        if (sanitized.startsWith("=")) {
            sanitized = sanitized.substring(1).trim();
        }
        if (justEvaluated || "0".equals(expression)) {
            expression = sanitized;
        } else if (endsWithOperator(expression)) {
            expression += sanitized;
        } else {
            expression = sanitized;
        }
        justEvaluated = false;
        lastCommittedResult = "";
        updateDisplay();
    }

    private void updateDisplay() {
        if (justEvaluated && !lastCommittedResult.isEmpty()) {
            tvExpression.setText(CalculatorEngine.formatExpressionForDisplay(expression));
            scrollExpression.post(() -> scrollExpression.fullScroll(HorizontalScrollView.FOCUS_RIGHT));
            tvResult.setTextColor(getColor(R.color.accent_orange));
            tvResult.setText(lastCommittedResult);
            return;
        }

        String previewTarget = trimTrailingOperator(expression);
        boolean hasOp = containsOperation(previewTarget) || endsWithOperator(expression);

        if (hasOp) {
            tvExpression.setText(CalculatorEngine.formatExpressionForDisplay(expression));
            scrollExpression.post(() -> scrollExpression.fullScroll(HorizontalScrollView.FOCUS_RIGHT));

            if (containsOperation(previewTarget)) {
                CalculatorEngine.EvaluationResult preview = CalculatorEngine.evaluate(previewTarget);
                if (preview.success) {
                    tvResult.setTextColor(getColor(R.color.text_secondary));
                    tvResult.setText("= " + preview.formattedResult);
                } else {
                    tvResult.setText("");
                }
            } else {
                CalculatorEngine.EvaluationResult preview = CalculatorEngine.evaluate(previewTarget);
                if (preview.success) {
                    tvResult.setTextColor(getColor(R.color.text_secondary));
                    tvResult.setText(preview.formattedResult);
                } else {
                    tvResult.setText("");
                }
            }
        } else {
            tvExpression.setText("");
            tvResult.setTextColor(getColor(R.color.text_primary));
            tvResult.setText(CalculatorEngine.formatExpressionForDisplay(expression));
        }
    }

    private boolean endsWithOperator(String expr) {
        if (expr.isEmpty()) {
            return false;
        }
        char last = expr.charAt(expr.length() - 1);
        return last == '+' || last == '−' || last == '-' || last == '×' || last == '÷';
    }

    private String trimTrailingOperator(String expr) {
        String trimmed = expr;
        while (!trimmed.isEmpty() && endsWithOperator(trimmed)) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private boolean containsOperation(String expr) {
        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if (c == '+' || c == '×' || c == '÷' || c == '%') {
                return true;
            }
            if ((c == '−' || c == '-') && i > 0) {
                return true;
            }
        }
        return false;
    }

    private boolean currentNumberHasComma() {
        for (int i = expression.length() - 1; i >= 0; i--) {
            char c = expression.charAt(i);
            if (c == ',' || c == '.') {
                return true;
            }
            if (c == '+' || c == '−' || c == '-' || c == '×' || c == '÷' || c == '%') {
                return false;
            }
        }
        return false;
    }

    private int findLastBinaryOperatorIndex(String expr) {
        for (int i = expr.length() - 1; i > 0; i--) {
            char c = expr.charAt(i);
            if (c == '+' || c == '×' || c == '÷') {
                return i;
            }
            if (c == '−' || c == '-') {
                char prev = expr.charAt(i - 1);
                if (prev != '+' && prev != '−' && prev != '-' && prev != '×' && prev != '÷') {
                    return i;
                }
            }
        }
        return -1;
    }
}