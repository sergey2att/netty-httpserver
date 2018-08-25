package com.silchenko.httpserver;

import java.util.ArrayList;
import java.util.List;

public class HistoryHolder {

    private List<String> history = new ArrayList<>();

    public List<String> getHistory() {
        return history;
    }

    public void setHistory(List<String> history) {
        this.history = history;
    }

    public void addRecord(String record) {
        this.history.add(record);
    }
}
