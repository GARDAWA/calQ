package com.example.kalkulator;

/**
 * Shared Intent Extra keys used across MainActivity, HistoryActivity, and DetailActivity.
 */
public final class IntentConstants {

    public static final String EXTRA_HISTORY_ID = "extra_history_id";
    public static final String EXTRA_EXPRESSION = "extra_expression";
    public static final String EXTRA_RESULT = "extra_result";
    public static final String EXTRA_TIMESTAMP = "extra_timestamp";
    public static final String EXTRA_CATEGORY = "extra_category";
    public static final String EXTRA_SELECTED_VALUE = "extra_selected_value";

    private IntentConstants() {
    }
}
