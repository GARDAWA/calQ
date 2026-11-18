package com.example.kalkulator;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

/**
 * Model representing one calculation entry stored in history.
 */
public class HistoryItem {

    public static final String CATEGORY_CALCULATOR = "Kalkulator";
    public static final String CATEGORY_PERCENTAGE = "Persentase";
    public static final String CATEGORY_DISCOUNT = "Diskon";
    public static final String CATEGORY_TEMPERATURE = "Suhu";

    private final String id;
    private final String expression;
    private final String result;
    private final long timestamp;
    private final String category;

    public HistoryItem(String expression, String result, String category) {
        this(UUID.randomUUID().toString(), expression, result, System.currentTimeMillis(), category);
    }

    public HistoryItem(String id, String expression, String result, long timestamp, String category) {
        this.id = id;
        this.expression = expression;
        this.result = result;
        this.timestamp = timestamp;
        this.category = category != null ? category : CATEGORY_CALCULATOR;
    }

    public String getId() {
        return id;
    }

    public String getExpression() {
        return expression;
    }

    public String getResult() {
        return result;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getCategory() {
        return category;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("id", id);
        obj.put("expression", expression);
        obj.put("result", result);
        obj.put("timestamp", timestamp);
        obj.put("category", category);
        return obj;
    }

    public static HistoryItem fromJson(JSONObject obj) throws JSONException {
        return new HistoryItem(
                obj.optString("id", UUID.randomUUID().toString()),
                obj.getString("expression"),
                obj.getString("result"),
                obj.optLong("timestamp", System.currentTimeMillis()),
                obj.optString("category", CATEGORY_CALCULATOR)
        );
    }
}
