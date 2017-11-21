package com.hermann.bussenliste;

import android.graphics.Bitmap;

import java.io.Serializable;
import java.util.ArrayList;

public class Player {

    private final long id;
    private final String name;
    private ArrayList<Fine> fines;
    private Bitmap photo;


    public Player(long id, String name) {
        this.id = id;
        this.name = name;
        this.fines = new ArrayList<>();
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public ArrayList<Fine> getFines() {
        return fines;
    }

    public void setFines(ArrayList<Fine> fines) {
        this.fines = fines;
    }

    public Bitmap getPhoto() {
        return photo;
    }

    public void setPhoto(Bitmap photo) {
        this.photo = photo;
    }

    public boolean hasPhoto() {
        if (getPhoto() == null) {
            return false;
        } else {
            return true;
        }
    }

    public int getTotalSumOfFines() {
        int totalSumOfFines = 0;

        for (Fine fine : fines) {
            totalSumOfFines += fine.getAmount();
        }
        return totalSumOfFines;
    }

    public void addFine(Fine fine) {
        fines.add(fine);
    }
}
