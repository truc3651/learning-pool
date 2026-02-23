package com.example;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

class Context {
    private final Map<String, Object> entries;

    public static final Context EMPTY = new Context(Collections.emptyMap());

    private Context(Map<String, Object> entries) {
        this.entries = Collections.unmodifiableMap(entries);
    }

    public Context put(String key, Object value) {
        Map<String, Object> newEntries = new HashMap<>(this.entries);
        newEntries.put(key, value);
        return new Context(newEntries);
    }

    @SuppressWarnings("unchecked")
    public <V> V get(String key) {
        return (V) entries.get(key);
    }

    @SuppressWarnings("unchecked")
    public <V> V getOrDefault(String key, V defaultValue) {
        return (V) entries.getOrDefault(key, defaultValue);
    }

    public boolean hasKey(String key) {
        return entries.containsKey(key);
    }

    @Override
    public String toString() {
        return "Context" + entries;
    }
}
