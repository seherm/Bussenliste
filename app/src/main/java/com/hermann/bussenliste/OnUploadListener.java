package com.hermann.bussenliste;

import org.json.JSONException;

/**
 * Created by sebas on 29.01.2018.
 */

public interface OnUploadListener {
    public void uploadTaskCompleted(String response);
    public void uploadTaskFailed(int statusCode);
    public void updateSQLiteDataFailed (JSONException e);
}
