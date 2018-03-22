package com.hermann.bussenliste;

import android.graphics.Bitmap;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

public class Player implements Comparable<Player> {

    private long id;
    private String name;
    private ArrayList<Fine> fines;
    private Bitmap photo;

    public Player(String name) {
        this.name = name;
        this.fines = new ArrayList<>();
    }

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
        fine.setDate(DateFormat.getDateInstance().format(new Date()));
        fines.add(fine);
    }

    @Override
    public int compareTo(Player player) {
        return this.name.compareTo(player.getName());
    }
}
