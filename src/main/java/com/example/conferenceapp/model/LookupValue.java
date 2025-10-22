package com.example.conferenceapp.model;

public class LookupValue {

    private final int id;
    private final String label;

    public LookupValue(int id, String label) {
        this.id = id;
        this.label = label;
    }

    public int getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
