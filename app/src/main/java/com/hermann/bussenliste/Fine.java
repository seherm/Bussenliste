package com.hermann.bussenliste;

import java.io.Serializable;

/**
 * Created by sebas on 05.09.2017.
 */

public class Fine implements Serializable{

    private long id;
    private String description;
    private int amount;

    public Fine(long id, String description, int amount) {
        this.id = id;
        this.description = description;
        this.amount = amount;
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
}
