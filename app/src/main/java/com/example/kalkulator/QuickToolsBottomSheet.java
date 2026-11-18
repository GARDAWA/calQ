package com.example.kalkulator;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.math.BigDecimal;

/**
 * Modal bottom sheet providing Quick Tools: Percentage, Discount, and Temperature conversion.
 */
public class QuickToolsBottomSheet extends BottomSheetDialogFragment {

    public static final String REQUEST_KEY = "quick_tools_request";
    public static final String RESULT_VALUE_KEY = "quick_tools_value";

    private enum ToolMode {
        PERCENTAGE, DISCOUNT, TEMPERATURE
    }

    private ToolMode currentMode = ToolMode.PERCENTAGE;
    private boolean isCelsiusToFahrenheit = true;

    private Chip chipPercentage;
    private Chip chipDiscount;
    private Chip chipTemperature;
    private TextInputLayout tilPrimary;
    private TextInputLayout tilSecondary;
    private TextInputEditText etPrimary;
    private TextInputEditText etSecondary;
    private MaterialButtonToggleGroup toggleTempUnit;
    private TextView tvToolSecondary;
    private TextView tvToolResult;
    private MaterialButton btnUseInCalc;

    private String latestExpression = "";
    private String latestNumericResult = "";
    private String latestCategory = HistoryItem.CATEGORY_PERCENTAGE;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_quick_tools, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ChipGroup chipGroup = view.findViewById(R.id.chipGroupTools);
        chipPercentage = view.findViewById(R.id.chipPercentage);
        chipDiscount = view.findViewById(R.id.chipDiscount);
        chipTemperature = view.findViewById(R.id.chipTemperature);

        tilPrimary = view.findViewById(R.id.tilInputPrimary);
        tilSecondary = view.findViewById(R.id.tilInputSecondary);
        etPrimary = view.findViewById(R.id.etInputPrimary);
        etSecondary = view.findViewById(R.id.etInputSecondary);
        toggleTempUnit = view.findViewById(R.id.toggleTempUnit);
        tvToolSecondary = view.findViewById(R.id.tvToolSecondary);
        tvToolResult = view.findViewById(R.id.tvToolResult);
        btnUseInCalc = view.findViewById(R.id.btnToolUseInCalc);

        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            int id = checkedIds.get(0);
            if (id == R.id.chipPercentage) {
                switchMode(ToolMode.PERCENTAGE);
            } else if (id == R.id.chipDiscount) {
                switchMode(ToolMode.DISCOUNT);
            } else if (id == R.id.chipTemperature) {
                switchMode(ToolMode.TEMPERATURE);
            }
        });

        toggleTempUnit.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                isCelsiusToFahrenheit = checkedId == R.id.btnCtoF;
                recalculate();
            }
        });

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                recalculate();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };
        etPrimary.addTextChangedListener(watcher);
        etSecondary.addTextChangedListener(watcher);

        btnUseInCalc.setOnClickListener(v -> applyResult());
        switchMode(ToolMode.PERCENTAGE);
    }
    private void switchMode(ToolMode mode) {
        currentMode = mode;
        etPrimary.setText("");
        etSecondary.setText("");

        int accentColor = requireContext().getColor(R.color.accent_orange);
        int divider = requireContext().getColor(R.color.divider_color);
        chipPercentage.setChipStrokeColor(ColorStateList.valueOf(
                mode == ToolMode.PERCENTAGE ? accentColor : divider));
        chipDiscount.setChipStrokeColor(ColorStateList.valueOf(
                mode == ToolMode.DISCOUNT ? accentColor : divider));
        chipTemperature.setChipStrokeColor(ColorStateList.valueOf(
                mode == ToolMode.TEMPERATURE ? accentColor : divider));

        if (mode == ToolMode.PERCENTAGE) {
            tilPrimary.setHint(getString(R.string.hint_percent));
            tilSecondary.setVisibility(View.VISIBLE);
            tilSecondary.setHint(getString(R.string.hint_base_value));
            toggleTempUnit.setVisibility(View.GONE);
            latestCategory = HistoryItem.CATEGORY_PERCENTAGE;
        } else if (mode == ToolMode.DISCOUNT) {
            tilPrimary.setHint(getString(R.string.hint_original_price));
            tilSecondary.setVisibility(View.VISIBLE);
            tilSecondary.setHint(getString(R.string.hint_discount_percent));
            toggleTempUnit.setVisibility(View.GONE);
            latestCategory = HistoryItem.CATEGORY_DISCOUNT;
        } else {
            tilPrimary.setHint(getString(R.string.hint_temperature_value));
            tilSecondary.setVisibility(View.GONE);
            toggleTempUnit.setVisibility(View.VISIBLE);
            latestCategory = HistoryItem.CATEGORY_TEMPERATURE;
        }
        recalculate();
    }

    private void recalculate() {
        BigDecimal first = parseDecimal(etPrimary.getText());
        BigDecimal second = parseDecimal(etSecondary.getText());

        if (currentMode == ToolMode.PERCENTAGE) {
            if (first == null || second == null) {
                setEmptyOutput(getString(R.string.calc_percentage_title));
                return;
            }
            BigDecimal result = CalculatorEngine.calculatePercentage(first, second);
            String formatted = CalculatorEngine.formatNumber(result);
            latestExpression = CalculatorEngine.formatNumber(first) + "% × "
                    + CalculatorEngine.formatNumber(second);
            latestNumericResult = formatted;
            tvToolSecondary.setText(latestExpression);
            tvToolResult.setText(formatted);
            btnUseInCalc.setEnabled(true);
        } else if (currentMode == ToolMode.DISCOUNT) {
            if (first == null || second == null) {
                setEmptyOutput(getString(R.string.calc_discount_title));
                return;
            }
            CalculatorEngine.DiscountResult res = CalculatorEngine.calculateDiscount(first, second);
            String savingsStr = CalculatorEngine.formatNumber(res.savings);
            String finalStr = CalculatorEngine.formatNumber(res.finalPrice);
            latestExpression = CalculatorEngine.formatNumber(first) + " − "
                    + CalculatorEngine.formatNumber(second) + "%";
            latestNumericResult = finalStr;
            tvToolSecondary.setText(getString(R.string.label_savings) + ": " + savingsStr);
            tvToolResult.setText(finalStr);
            btnUseInCalc.setEnabled(true);
        } else {
            if (first == null) {
                setEmptyOutput(getString(R.string.calc_temperature_title));
                return;
            }
            BigDecimal converted = isCelsiusToFahrenheit
                    ? CalculatorEngine.celsiusToFahrenheit(first)
                    : CalculatorEngine.fahrenheitToCelsius(first);
            String formatted = CalculatorEngine.formatNumber(converted);
            String fromUnit = isCelsiusToFahrenheit ? "°C" : "°F";
            String toUnit = isCelsiusToFahrenheit ? "°F" : "°C";
            latestExpression = CalculatorEngine.formatNumber(first) + fromUnit + " → " + toUnit;
            latestNumericResult = formatted;
            tvToolSecondary.setText(latestExpression);
            tvToolResult.setText(formatted + " " + toUnit);
            btnUseInCalc.setEnabled(true);
        }
    }

    private void setEmptyOutput(String title) {
        latestExpression = "";
        latestNumericResult = "";
        tvToolSecondary.setText(title);
        tvToolResult.setText("0");
        btnUseInCalc.setEnabled(false);
    }

    private BigDecimal parseDecimal(@Nullable Editable editable) {
        if (editable == null) {
            return null;
        }
        String text = editable.toString().trim();
        if (text.isEmpty() || "-".equals(text) || ".".equals(text) || "-.".equals(text)) {
            return null;
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void applyResult() {
        if (latestNumericResult.isEmpty()) {
            return;
        }
        HistoryRepository repo = new HistoryRepository(requireContext());
        repo.add(new HistoryItem(latestExpression, latestNumericResult, latestCategory));

        Bundle bundle = new Bundle();
        bundle.putString(RESULT_VALUE_KEY, latestNumericResult);
        getParentFragmentManager().setFragmentResult(REQUEST_KEY, bundle);
        dismiss();
    }
}
