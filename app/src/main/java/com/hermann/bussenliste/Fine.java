package com.hermann.bussenliste;

import java.io.Serializable;

/**
 * Created by sebas on 05.09.2017.
 */

public class Fine implements Serializable{

    private FineType type;
    private int amount;
    private int descriptionStringResourceId;

    public Fine(FineType type, int amount, int descriptionStringResourceId) {
        this.type = type;
        this.amount = amount;
        this.descriptionStringResourceId = descriptionStringResourceId;
    }

    public int getAmount() {
        return amount;
    }

    public int getDescriptionStringResourceId() {
        return descriptionStringResourceId;
    }
}
