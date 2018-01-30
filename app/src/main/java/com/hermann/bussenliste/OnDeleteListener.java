package com.hermann.bussenliste;

/**
 * Created by sebas on 30.01.2018.
 */

public interface OnDeleteListener {
    public void deleteTaskCompleted(Player player);
    public void deleteTaskFailed(int statusCode);
}
