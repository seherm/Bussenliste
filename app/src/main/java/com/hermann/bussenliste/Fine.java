package com.hermann.bussenliste;

import java.io.Serializable;

/**
 * Created by sebas on 05.09.2017.
 */

public class Fine implements Serializable{

    private FineType type;
    private int amount;

    public Fine(FineType type, int amount) {
        this.type = type;
        this.amount = amount;
    }

    public FineType getType() {
        return type;
    }

    public int getAmount() {
        return amount;
    }
}
