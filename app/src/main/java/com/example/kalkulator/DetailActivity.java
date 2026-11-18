package com.example.kalkulator;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Displays detailed information about a selected calculation and lets the user
 * reuse its result in MainActivity or copy its expression/result.
 */
public class DetailActivity extends AppCompatActivity {

    private String expression = "";
    private String result = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_detail);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.detailRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Intent intent = getIntent();
        expression = intent.getStringExtra(IntentConstants.EXTRA_EXPRESSION);
        result = intent.getStringExtra(IntentConstants.EXTRA_RESULT);
        String category = intent.getStringExtra(IntentConstants.EXTRA_CATEGORY);
        long timestamp = intent.getLongExtra(IntentConstants.EXTRA_TIMESTAMP, System.currentTimeMillis());

        if (expression == null) {
            expression = "";
        }
        if (result == null) {
            result = "0";
        }
        if (category == null || category.isEmpty()) {
            category = HistoryItem.CATEGORY_CALCULATOR;
        }

        TextView tvCategory = findViewById(R.id.tvDetailCategory);
        TextView tvTimestamp = findViewById(R.id.tvDetailTimestamp);
        TextView tvExpression = findViewById(R.id.tvDetailExpression);
        TextView tvResult = findViewById(R.id.tvDetailResult);

        SimpleDateFormat fullDateFormat =
                new SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.getDefault());

        tvCategory.setText(category);
        tvTimestamp.setText(fullDateFormat.format(new Date(timestamp)));
        tvExpression.setText(CalculatorEngine.formatExpressionForDisplay(expression));
        tvResult.setText(CalculatorEngine.formatNumberString(result));

        MaterialButton btnBack = findViewById(R.id.btnBack);
        MaterialButton btnDelete = findViewById(R.id.btnDelete);
        MaterialButton btnUseInCalculator = findViewById(R.id.btnUseInCalculator);
        MaterialButton btnCopyExpression = findViewById(R.id.btnCopyExpression);
        MaterialButton btnCopyResult = findViewById(R.id.btnCopyResult);

        btnBack.setOnClickListener(v -> finish());

        String historyId = intent.getStringExtra(IntentConstants.EXTRA_HISTORY_ID);
        if (historyId != null && !historyId.isEmpty()) {
            btnDelete.setVisibility(View.VISIBLE);
            btnDelete.setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.confirm_delete_item_title)
                    .setMessage(R.string.confirm_delete_item_msg)
                    .setNegativeButton(R.string.action_cancel, null)
                    .setPositiveButton(R.string.action_delete, (dialog, which) -> {
                        HistoryRepository repo = new HistoryRepository(this);
                        repo.deleteById(historyId);
                        finish();
                    })
                    .show());
        } else {
            btnDelete.setVisibility(View.GONE);
        }

        btnUseInCalculator.setOnClickListener(v -> {
            Intent data = new Intent();
            data.putExtra(IntentConstants.EXTRA_SELECTED_VALUE, result);
            setResult(RESULT_OK, data);
            finish();
        });

        btnCopyExpression.setOnClickListener(v -> copyToClipboard("Ekspresi", expression));
        btnCopyResult.setOnClickListener(v -> copyToClipboard("Hasil", result));
    }

    private void copyToClipboard(String label, String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText(label, text));
            Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
        }
    }
}
