package com.hermann.bussenliste;

import java.io.Serializable;

public class Fine {

    private final long id;
    private final String description;
    private final int amount;
    private final String date;

    public Fine(long id, String description, int amount, String date) {
        this.id = id;
        this.description = description;
        this.amount = amount;
        this.date = date;
    }

    public long getId() { return id; }

    public String getDescription() {
        return description;
    }

    public int getAmount() {
        return amount;
    }

    public String getDate() {
        return date;
    }
}
