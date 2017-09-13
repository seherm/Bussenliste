package com.hermann.bussenliste;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by sebas on 04.09.2017.
 */

public class Player implements Serializable {

    private long id;
    private String name;
    private ArrayList<Fine> fines;
    private int imageLayoutResourceId;


    public Player(long id, String name) {
        this.id = id;
        this.name = name;
        this.fines = new ArrayList<>();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<Fine> getFines() {
        return fines;
    }

    public void setFines(ArrayList<Fine> fines) {
        this.fines = fines;
    }

    public int getImageLayoutResourceId() {
        return imageLayoutResourceId;
    }

    public void setImageLayoutResourceId(int imageLayoutResourceId) {
        this.imageLayoutResourceId = imageLayoutResourceId;
    }

    public int getTotalSumOfFines() {
        int totalSumOfFines = 0;

        for (Fine fine : fines) {
            totalSumOfFines += fine.getAmount();
        }
        return totalSumOfFines;
    }

    public void addFine(FineType fineType) {
        Fine fine;
        switch (fineType) {
            case VERSPÄTUNG:
                fine = new Fine(FineType.VERSPÄTUNG, 5);
                fines.add(fine);
                break;
            case VERGESSENESMATERIAL:
                fine = new Fine(FineType.VERGESSENESMATERIAL, 5);
                fines.add(fine);
                break;
            default:
                break;
        }
    }
}
