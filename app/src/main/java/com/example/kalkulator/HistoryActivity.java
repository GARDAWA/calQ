package com.example.kalkulator;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

/**
 * Displays and manages persisted calculation history.
 */
public class HistoryActivity extends AppCompatActivity {

    private HistoryRepository repository;
    private HistoryAdapter adapter;
    private RecyclerView rvHistory;
    private LinearLayout emptyState;
    private MaterialButton btnClearAll;

    private final ActivityResultLauncher<Intent> detailLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String selectedValue = result.getData()
                            .getStringExtra(IntentConstants.EXTRA_SELECTED_VALUE);
                    if (selectedValue != null) {
                        returnValueToCalculator(selectedValue);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_history);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.historyRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        repository = new HistoryRepository(this);

        rvHistory = findViewById(R.id.rvHistory);
        emptyState = findViewById(R.id.emptyState);
        btnClearAll = findViewById(R.id.btnClearAll);
        MaterialButton btnBack = findViewById(R.id.btnBack);

        adapter = new HistoryAdapter(new HistoryAdapter.OnHistoryActionListener() {
            @Override
            public void onItemClick(HistoryItem item) {
                openDetail(item);
            }

            @Override
            public void onUseClick(HistoryItem item) {
                returnValueToCalculator(item.getResult());
            }
        });

        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        rvHistory.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());
        btnClearAll.setOnClickListener(v -> confirmClearHistory());

        loadHistory();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadHistory();
    }

    private void loadHistory() {
        List<HistoryItem> items = repository.getAll();
        adapter.submitList(items);
        boolean isEmpty = items.isEmpty();
        emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        rvHistory.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        btnClearAll.setEnabled(!isEmpty);
        btnClearAll.setAlpha(isEmpty ? 0.35f : 1.0f);
    }

    private void openDetail(HistoryItem item) {
        Intent intent = new Intent(this, DetailActivity.class);
        intent.putExtra(IntentConstants.EXTRA_HISTORY_ID, item.getId());
        intent.putExtra(IntentConstants.EXTRA_EXPRESSION, item.getExpression());
        intent.putExtra(IntentConstants.EXTRA_RESULT, item.getResult());
        intent.putExtra(IntentConstants.EXTRA_TIMESTAMP, item.getTimestamp());
        intent.putExtra(IntentConstants.EXTRA_CATEGORY, item.getCategory());
        detailLauncher.launch(intent);
    }

    private void returnValueToCalculator(String value) {
        Intent data = new Intent();
        data.putExtra(IntentConstants.EXTRA_SELECTED_VALUE, value);
        setResult(RESULT_OK, data);
        finish();
    }

    private void confirmClearHistory() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.confirm_clear_history_title)
                .setMessage(R.string.confirm_clear_history_msg)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete, (dialog, which) -> {
                    repository.clearAll();
                    loadHistory();
                })
                .show();
    }
}
