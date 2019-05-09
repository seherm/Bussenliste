package com.hermann.bussenliste.domain;

public class Fine {

    private long id;
    private String description;
    private int amount;
    private String date;

    public Fine(long id, String description, int amount) {
        this.id = id;
        this.description = description;
        this.amount = amount;
    }

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

    public void setDate(String date) {
        this.date = date;
    }
}
