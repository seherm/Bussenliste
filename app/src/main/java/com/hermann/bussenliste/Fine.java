package com.hermann.bussenliste;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by sebas on 05.09.2017.
 */

public class Fine implements Serializable{

    private long id;
    private String description;
    private int amount;
    private String date;

    public Fine(long id, String description, int amount, String date) {
        this.id = id;
        this.description = description;
        this.amount = amount;
        this.date = date;
    }

    public long getId() {
        return id;
    }

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
