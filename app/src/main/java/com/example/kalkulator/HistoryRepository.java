package com.example.kalkulator;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Persists calculation history locally using SharedPreferences and JSON.
 */
public class HistoryRepository {

    private static final String PREFS_NAME = "calq_history_prefs";
    private static final String KEY_HISTORY_LIST = "history_items";
    private static final int MAX_ITEMS = 100;

    private final SharedPreferences prefs;

    public HistoryRepository(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public List<HistoryItem> getAll() {
        List<HistoryItem> items = new ArrayList<>();
        String raw = prefs.getString(KEY_HISTORY_LIST, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                items.add(HistoryItem.fromJson(obj));
            }
        } catch (JSONException ignored) {
            // Return whatever parsed cleanly
        }
        return items;
    }

    public void add(HistoryItem item) {
        if (item == null) {
            return;
        }
        List<HistoryItem> items = getAll();
        items.add(0, item);
        if (items.size() > MAX_ITEMS) {
            items = new ArrayList<>(items.subList(0, MAX_ITEMS));
        }
        saveList(items);
    }

    public void deleteById(String id) {
        if (id == null) {
            return;
        }
        List<HistoryItem> items = getAll();
        List<HistoryItem> filtered = new ArrayList<>();
        for (HistoryItem item : items) {
            if (!id.equals(item.getId())) {
                filtered.add(item);
            }
        }
        saveList(filtered);
    }

    public void clearAll() {
        prefs.edit().remove(KEY_HISTORY_LIST).apply();
    }

    private void saveList(List<HistoryItem> items) {
        JSONArray array = new JSONArray();
        for (HistoryItem item : items) {
            try {
                array.put(item.toJson());
            } catch (JSONException ignored) {
            }
        }
        prefs.edit().putString(KEY_HISTORY_LIST, array.toString()).apply();
    }
}
