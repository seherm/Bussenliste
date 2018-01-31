package com.hermann.bussenliste;

import org.json.JSONException;

public interface OnServerTaskListener {
    void deletePlayerTaskCompleted(Player player);
    void deletePlayerTaskFailed(int statusCode);
    void deleteFineTaskCompleted(Fine fine);
    void deleteFineTaskFailed(int statusCode);
    void downloadTaskCompleted (String response);
    void downloadTaskFailed (int statusCode);
    void updateSyncStatusFailed (JSONException e);
    void uploadTaskCompleted(String response);
    void uploadTaskFailed(int statusCode);
    void updateSQLiteDataFailed (JSONException e);
}
